package com.nellex.helpers;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

/**
 * Class comprising of helper functions for processing text and titles from wiki
 * data set
 * 
 * @author nellex
 */
public class TokenHelper {

	static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();

	public static boolean isPureAscii(String v) {
		return asciiEncoder.canEncode(v);
	}

	public static String processToken(final String token, boolean shouldStem) {
		String word = token.trim();
		word = word.replace(" ", "").replace("\t", "");

		if (shouldStem) {
			Stemmer stemmer = new Stemmer();
			word = stemmer.stem(word);
		}

		if (word.contains(System.getProperty("line.separator")))
			return "";

		word = word.toLowerCase();
		word = StringUtils.stripAccents(word);

		if (word.isEmpty() || !isPureAscii(word))
			return "";

		while (!word.isEmpty()) {
			char ch = word.charAt(0);
			if (!Character.isLetter(ch) && !Character.isDigit(ch))
				word = word.substring(1);
			else
				break;
		}
		while (!word.isEmpty()) {
			char ch = word.charAt(word.length() - 1);
			if (!Character.isLetter(ch) && !Character.isDigit(ch))
				word = word.substring(0, word.length() - 1);
			else
				break;
		}

		if (word.isEmpty() || !isPureAscii(word))
			return "";
		return word;
	}

	public static String processTitle(final String title) {
		final String unescape = Jsoup.parse(title).text();
		return StringUtils.stripAccents(unescape.replace(' ', '_'));
	}

}
