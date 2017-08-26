package com.nellex.hadoop.indexer;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class WikiDataReducer extends Reducer<Text, Text, Text, Text> {
	private static DecimalFormat df;
	// Estimate on total number of documents to process
	final private static double NUM_OF_DOCUMENTS = 10387413;

	private void setup() throws IOException {
		df = new DecimalFormat("#.######");
		df.setRoundingMode(RoundingMode.CEILING);
	}

	@Override
	public void reduce(Text key, Iterable<Text> values, Context context)
			throws IOException, InterruptedException {
		setup();

		int counter = 0;
		int numOfPages = 0;
		boolean first = true;

		List<Text> cache = new LinkedList<Text>();
		String occur = "";

		for (Text value : values) {
			numOfPages++;
			if (counter == 0)
				context.progress();
			counter = (counter + 1) % 1000;

			if (numOfPages > (0.2 * NUM_OF_DOCUMENTS))
				return;
			cache.add(value);
		}

		if (numOfPages > (0.2 * NUM_OF_DOCUMENTS))
			return;

		for (Text value : cache) {
			String[] params = value.toString().split("\\|");
			final String title = params[0];
			final Integer streamLength = Integer.parseInt(params[1]);
			final Integer freq = Integer.parseInt(params[2]);

			double TF = 1.0 * freq / streamLength;
			if (!first)
				occur += ";";
			occur += "{" + title + "}[" + freq + "," + df.format(TF) + "]";
			first = false;

			if (counter == 0)
				context.progress();
			counter = (counter + 1) % 1000;
		}

		double IDF = Math.log(1.0 * NUM_OF_DOCUMENTS / numOfPages);
		occur = df.format(IDF) + ";" + occur;

		occur = occur.trim();
		if (!occur.isEmpty())
			context.write(key, new Text(occur));
	}
}
