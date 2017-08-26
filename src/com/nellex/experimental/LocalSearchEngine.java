package com.nellex.experimental;

import static com.nellex.helpers.DataCommon.ARTIFACTS_DIR;
import static com.nellex.helpers.DataCommon.RANKING_MODEL;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.nellex.helpers.FeatureExtractor;

import ciir.umass.edu.eval.Evaluator;

/**
 * Class designed to fetch search results for a given query locally
 * 
 * @author nellex
 */
public class LocalSearchEngine {

	static Map<Integer, String> titleMap = null;

	public static Map<Integer, String> parseFeature(final String data) {
		Map<Integer, String> featureMap = new HashMap<Integer, String>();

		int lineNum = 0;
		String[] lines = data.split("\n");

		for (String line : lines) {
			if (!line.contains("qid"))
				continue;
			featureMap.put(lineNum, line);
			++lineNum;
		}
		return featureMap;
	}

	public static void main(String[] args) throws SQLException, IOException {
		Scanner in = new Scanner(System.in);
		String str;
		while ((str = in.nextLine()) != null) {
			File testFile = new File(ARTIFACTS_DIR + "test.dat");

			final FileWriter writer = new FileWriter(testFile);
			final String row = FeatureExtractor.getFeatureSet(str, null);
			writer.write(row);
			writer.close();

			final File tmpFile = new File(ARTIFACTS_DIR + "pred.dat");
			Map<Integer, String> qRow = parseFeature(row);

			final String arg = "-load " + RANKING_MODEL + " -rank " + testFile.getAbsolutePath()
					+ " -score " + tmpFile.getAbsolutePath();
			Evaluator.main(arg.split(" "));

			final Scanner predFile = new Scanner(tmpFile);
			final List<Pair<Double, String>> result = new ArrayList<Pair<Double, String>>();

			while (predFile.hasNextLine()) {
				final String line = predFile.nextLine();
				final String[] params = line.split("\t");

				final double score = Double.parseDouble(params[2]);
				final int id = Integer.parseInt(params[1]);

				final String title = qRow.get(id).split("#")[1];

				result.add(new ImmutablePair<Double, String>(score, title));
			}

			Collections.sort(result);

			System.out.println("\n\nResult: ");
			for (Pair<Double, String> pair : result) {
				System.out.println(pair.getRight() + " " + pair.getLeft());
			}
			predFile.close();
		}
		in.close();
	}
}
