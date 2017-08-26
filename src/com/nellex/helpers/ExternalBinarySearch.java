package com.nellex.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

/**
 * Class to perform binary search for a given string over a large file
 * 
 * @author nellex
 */
public class ExternalBinarySearch {
	private static final long PAGE_SIZE = Integer.MAX_VALUE;
	private List<MappedByteBuffer> buffers = new ArrayList<MappedByteBuffer>();
	private final byte raw[] = new byte[1];
	private final File file;
	private final String delimiter;

	public static void main(String[] args) throws IOException {
		final String fileName = "/home/local/ANT/nilessah/all_titles.txt";
		ExternalBinarySearch fileModel = new ExternalBinarySearch(new File(fileName), "\\|");
		Scanner in = new Scanner(System.in);

		System.out.println("Search");
		while (in.hasNext()) {
			String inp = in.next();
			System.out.println(fileModel.search(inp));
		}
		in.close();
	}

	public ExternalBinarySearch(final File file, final String delimiter) throws IOException {
		this.file = file;
		this.delimiter = delimiter;
		@SuppressWarnings("resource")
		final FileChannel channel = (new FileInputStream(file)).getChannel();
		long start = 0, length = 0;
		for (long index = 0; start + length < channel.size(); index++) {
			if ((channel.size() / PAGE_SIZE) == index)
				length = (channel.size() - index * PAGE_SIZE);
			else
				length = PAGE_SIZE;
			start = index * PAGE_SIZE;
			buffers.add(channel.map(MapMode.READ_ONLY, start, length));
		}
		channel.close();
	}

	private String getString(long bytePosition) {
		int page = (int) (bytePosition / PAGE_SIZE);
		int index = (int) (bytePosition % PAGE_SIZE);
		raw[0] = buffers.get(page).get(index);
		return new String(raw);
	}

	public String search(final String str) throws IOException {
		long startByte = 0;
		long endByte = file.length();

		try {
			while (startByte <= endByte) {
				long position = (startByte + endByte) / 2;
				String newline = System.getProperty("line.separator");

				String candidate = this.getString(--position);
				while (position >= 0 && !candidate.equals(newline))
					candidate = this.getString(--position);

				StringBuilder tokenBuilder = new StringBuilder();
				candidate = "";
				long cur = position;
				while (!candidate.equals(newline)) {
					if (!candidate.isEmpty())
						tokenBuilder.append(candidate.charAt(0));
					candidate = this.getString(++cur);
				}

				final String token = StringUtils.strip(tokenBuilder.toString());
				String word = token.split(delimiter)[0];
				String attr = token.split(delimiter)[1];

				if (str.equalsIgnoreCase(word)) {
					return attr;
				} else if (str.compareTo(word) < 0) {
					endByte = position - 1;
				} else {
					startByte = cur + 1;
				}

				// System.out.println(startByte + " " + endByte + " [" + word +
				// "] => " + attr
				// + " -- " + str.compareTo(token));
			}
		} catch (Exception e) {
			System.out.println(
					"Exception thrown for string " + str + " at " + startByte + " " + endByte);
			return "";
		}

		return "";
	}
}