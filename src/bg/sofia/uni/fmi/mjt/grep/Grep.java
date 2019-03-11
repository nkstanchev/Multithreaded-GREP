package bg.sofia.uni.fmi.mjt.grep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Grep {

	private String searchString;
	private Path dirPath;
	private int numOfThreads;
	private Path pathToOutput;
	private boolean outputToPath;
	private boolean searchOnlyWords;
	private boolean ignoreCaseSensitivity;
	private Queue<String> results;
	private final ExecutorService pool;
	private List<CompletableFuture<Void>> futures;

	public Grep(String searchString, String dirPath, String numOfThreads, String pathToOutput) throws Exception {

		this.searchString = searchString;
		this.results = new LinkedBlockingQueue<String>();

		// validate path to directory tree
		Path pathToDir = Paths.get(dirPath);
		if (Files.exists(pathToDir)) {
			this.dirPath = pathToDir;
		} else {
			throw new Exception("Error: A non valid path was given!");
		}
		// validate and setup output file
		if (!pathToOutput.equals("")) {
			Path pathToOutputObj = Paths.get(pathToOutput);
			if (Files.exists(pathToOutputObj)) {
				this.pathToOutput = pathToOutputObj;
				this.outputToPath = true;
			} else {
				throw new Exception("Error: A non valid path for output file was given!");
			}
		}
		// setup multithreading
		this.numOfThreads = Integer.parseInt(numOfThreads);
		this.pool = Executors.newWorkStealingPool(this.numOfThreads);
		futures = new CopyOnWriteArrayList<>();

	}

	public Grep(String searchString, String dirPath, String numOfThreads, String pathToOutput, String optionalFlags)
			throws Exception {

		this(searchString, dirPath, numOfThreads, pathToOutput);
		// handle optional parameters
		if (optionalFlags.contains("w")) {
			this.searchOnlyWords = true;
		}
		if (optionalFlags.contains("i")) {
			this.ignoreCaseSensitivity = true;
			searchString = searchString.toLowerCase();
		}
		this.searchString = searchString;
	}

	/*
	 * Searching through file
	 */
	public void search(File file) {
		Runnable runner = new Runnable() {
			public void run() {
				list(file);
			}
		};
		CompletableFuture<Void> future = CompletableFuture.runAsync(runner, this.pool);
		futures.add(future);
	}

	/*
	 * Searching through directory tree
	 */
	public void search(Path path) {
		for (String filename : path.toFile().list()) {
			String filePath = path.resolve(filename).toString();
			File file = new File(filePath);
			if (file.isDirectory()) {
				Runnable runner = new Runnable() {
					public void run() {
						search(Paths.get(filePath));
					}
				};
				CompletableFuture<Void> future = CompletableFuture.runAsync(runner, this.pool);
				futures.add(future);
			} else {
				search(file);
			}
		}
	}

	/*
	 * Main Search Method
	 */
	public void search() {
		// i need to check if file is non binary

		if (Files.isDirectory(dirPath)) {
			this.search(dirPath);
		} else {
			this.search(dirPath.toFile());
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();

		// After all threads have finished their job searching.
		// Either print to console or output to file
		if (this.outputToPath) {

			if (Files.isDirectory(this.pathToOutput)) {
				System.out.println("Error: Supplied path must lead to a file, not a directory!");
				return;
			}
			try (PrintWriter writer = new PrintWriter(new FileWriter(this.pathToOutput.toFile()));) {
				for (String line : this.results) {
					writer.println(line);
				}
			} catch (IOException e) {
				System.out.println("Error: Failed opening the file. Make sure the file "
						+ "exists and you have the right permissions");
			}
		} else {
			for (String line : this.results) {
				System.out.println(line);
			}

		}
	}

	/*
	 * Method for adding a file's matching results to this class' Queue<String>
	 * results
	 */
	public void list(File file) {

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			int lineNumber = 1;
			while ((line = br.readLine()) != null) {

				boolean isPresent = false;
				String temp = line;
				if (this.ignoreCaseSensitivity) {
					temp.toLowerCase();
				}
				if (this.searchOnlyWords) {

					String validPattern = "\\b" + this.searchString + "\\b";
					Pattern pattern = Pattern.compile(validPattern);
					Matcher matcher = pattern.matcher(temp);
					if (matcher.find()) {
						isPresent = true;
					}
				} else {
					if (temp.contains(searchString)) {
						isPresent = true;
					}
				}
				if (isPresent) {
					String output = file.getAbsolutePath() + ":" + lineNumber + ":" + line;
					this.results.add(output);
				}
				lineNumber++;
			}
		} catch (FileNotFoundException e) {
			System.out.println("Error: Failed opening the file. File does not exist ");

		} catch (IOException e) {
			System.out.println("Error: Failed opening the file. Make sure the file "
					+ "exists and you have the right permissions");
		}
	}
}
