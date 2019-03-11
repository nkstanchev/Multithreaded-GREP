package bg.sofia.uni.fmi.mjt.grep;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	static final int FIRST = 1;
	static final int SECOND = 2;
	static final int THIRD = 3;
	static final int FOURTH = 4;
	static final int FIFTH = 5;
	static final int SIXTH = 6;
	
	public static void parse(List<String> words) throws Exception {
		
		
		
		if (!words.get(0).equals("grep")) {
			throw new Exception("Unrecognized command!");
		}
		String pathToOutput = "";
		
		if (words.size() == FIFTH) {
			pathToOutput = words.get(FOURTH);
		}
		Grep grep = new Grep(words.get(FIRST), words.get(SECOND), words.get(THIRD), pathToOutput);
		grep.search();
	}
	
	public static void parseWithOptional(List<String> words) throws Exception {
		
		final int first = 1;
		final int second = 2;
		final int third = 3;
		final int fourth = 4;
		final int fifth = 5;
		final int sixth = 6;
		
		if (!words.get(0).equals("grep")) {
			throw new Exception("Unrecognized command!");
		}
		String pathToOutput = "";
		if (words.size() == SIXTH) {
			pathToOutput = words.get(FIFTH);
		}
		Grep grep = new Grep(words.get(SECOND), words.get(THIRD), words.get(FOURTH), pathToOutput, words.get(FIRST));
		grep.search();
	}
	
	public static boolean hasOptional(List<String> words) {
		return words.get(FIRST).indexOf('-') == 0;
	}

	public static void main(String... args) throws Exception {

		Scanner s = new Scanner(System.in);
		while (true) {
			System.out.println("Enter command");
			String cmd = s.nextLine();

			List<String> matchList = new ArrayList<String>();
			Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
			Matcher regexMatcher = regex.matcher(cmd);
			while (regexMatcher.find()) {
				if (regexMatcher.group(1) != null) {
					// Add double-quoted string without the quotes
					matchList.add(regexMatcher.group(1));
				} else if (regexMatcher.group(2) != null) {
					// Add single-quoted string without the quote s
					matchList.add(regexMatcher.group(2));
				} else {
					// Add unquoted word
					matchList.add(regexMatcher.group());
				}
			}
			if (hasOptional(matchList)) {
				parseWithOptional(matchList);
			} else {
				parse(matchList);
			}

		}
	}
}
