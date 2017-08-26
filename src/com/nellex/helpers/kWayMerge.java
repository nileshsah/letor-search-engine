package com.nellex.helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Class to merge different part files produced by Hadoop runs, alternative to
 * 'sort -m' unix command
 * 
 * @author nellex
 */
public class kWayMerge {

	private static List<String> getFiles(final String directory) {
		List<String> results = new ArrayList<String>();
		File[] files = new File(directory).listFiles();

		for (File file : files)
			if (file.isFile() && file.getName().contains("part-"))
				results.add(file.getAbsolutePath());
		return results;
	}

	public static void merge(final String inputDir, final String outFile) throws IOException {
		final List<String> partFiles = getFiles(inputDir);
		final int n = partFiles.size();
		Scanner[] in = new Scanner[n];
		String[] curLine = new String[n];

		for (int i = 0; i < n; i++) {
			in[i] = new Scanner(new File(partFiles.get(i)));
			curLine[i] = null;
			if (in[i].hasNextLine())
				curLine[i] = in[i].nextLine();
			System.out.println("FILE: " + partFiles.get(i));
		}

		FileWriter writer = new FileWriter(outFile);
		boolean isLeft = true;

		while (isLeft) {
			int idx = -1;
			for (int i = 0; i < n; i++) {
				if (curLine[i] == null)
					continue;
				if (idx == -1) {
					idx = i;
					continue;
				}
				if (curLine[i].compareTo(curLine[idx]) < 0)
					idx = i;
			}

			if (idx == -1) {
				isLeft = false;
				break;
			}

			writer.write(curLine[idx] + '\n');
			if (in[idx].hasNextLine())
				curLine[idx] = in[idx].nextLine();
			else
				curLine[idx] = null;
		}
		writer.close();
	}

	public static void main(String[] args) throws IOException {
		merge("/tmp/Hadoop", "/tmp/Hadoop/merge");
	}
}
