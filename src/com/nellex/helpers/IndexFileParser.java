package com.nellex.helpers;

import static com.nellex.helpers.DataCommon.ARTIFACTS_DIR;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Class to process the index file generated from the Hadoop run for easy import
 * of required data into db
 * 
 * @author nellex
 */
public class IndexFileParser {

	public static Map<String, Integer> getTitleMap(final String titlePath) throws Exception {
		System.out.println("Caching title data ..");
		Map<String, Integer> titleMap = new HashMap<String, Integer>();
		CSVReader reader = new CSVReader(new FileReader(titlePath), ',');

		String[] nextLine;

		while ((nextLine = reader.readNext()) != null) {
			try {
				final int id = Integer.parseInt(nextLine[0]);
				final String title = TokenHelper.processTitle(nextLine[1]);
				titleMap.put(title, id);
			} catch (Exception ex) {
				System.out.println(nextLine);
				ex.printStackTrace();
				reader.close();
				throw ex;
			}
		}
		System.out.println("Caching completed!");
		reader.close();

		return titleMap;
	}

	public static Map<Integer, String> getIdToTitleMap(final String titlePath) throws Exception {
		System.out.println("Caching title data ..");
		Map<Integer, String> titleMap = new HashMap<Integer, String>();
		CSVReader reader = new CSVReader(new FileReader(titlePath), ',');

		String[] nextLine;

		while ((nextLine = reader.readNext()) != null) {
			try {
				final int id = Integer.parseInt(nextLine[0]);
				final String title = TokenHelper.processTitle(nextLine[1]);
				titleMap.put(id, title);
			} catch (Exception ex) {
				System.out.println(nextLine);
				ex.printStackTrace();
				reader.close();
				throw ex;
			}
		}
		System.out.println("Caching completed!");
		reader.close();

		return titleMap;
	}

	/**
	 * The function is responsible for producing a tsv of the same schema as that of
	 * the pageIndex table for easy import to db
	 * 
	 * @param filePath
	 *            The path to the generated index file from Hadoop Indexer run
	 * @param titlePath
	 *            The path to a csv file which stores the title name along with its
	 *            respective id as in the enwiki xml dataset e.g: 1,Asia 2,Sun Burn
	 * @throws Exception
	 */
	public static void parseFile(final String filePath, final String titlePath) throws Exception {
		DecimalFormat df = new DecimalFormat("#.######");
		df.setRoundingMode(RoundingMode.CEILING);
		Scanner in = new Scanner(new File(filePath));

		Map<String, Integer> stopWordsMap = new HashMap<String, Integer>();
		Scanner sin = new Scanner(new File(ARTIFACTS_DIR + "stop_words.txt"));
		while (sin.hasNextLine()) {
			final String tok = TokenHelper.processToken(sin.nextLine(), true);
			stopWordsMap.put(tok, 1);
		}
		sin.close();

		Map<String, Integer> titleMap = getTitleMap(titlePath);

		// Rough estimate on the total number of titles to process for logging progress
		final double total = 4951197;
		double last = 0;
		long lineNumber = 0;

		FileWriter wr = new FileWriter(new File(ARTIFACTS_DIR + "pageIndex.tsv"));
		double num = 0, den = 0;

		while (in.hasNextLine()) {
			lineNumber++;
			if ((lineNumber / total) * 100 > last) {
				System.out.println("Completed: " + (lineNumber / total) * 100 + "%");
				last += 1;
			}
			final String line = in.nextLine();
			final String[] params = line.split("\t");

			final String keyword = params[0];
			final String[] args = params[1].split("\\];");

			if (stopWordsMap.containsKey(keyword))
				continue;

			final double IDF = Double.parseDouble(params[1].split(";")[0]);

			List<Long> pageIds = new LinkedList<Long>();
			List<Integer> numOfOccurence = new LinkedList<Integer>();
			List<String> TFs = new LinkedList<String>();

			for (int i = 1; i < args.length; i++) {
				final String arg = args[i];

				final String title = TokenHelper.processTitle(arg.split("}")[0].substring(1));
				final String[] attr = arg.split("\\[")[1].split("\\]")[0].split(",");

				den++;
				if (!titleMap.containsKey(title)) {
					num++;
					continue;
				}

				final long pageId = titleMap.get(title);
				final int occur = Integer.parseInt(attr[0]);
				final double TF = Double.parseDouble(attr[1]);

				pageIds.add(pageId);
				numOfOccurence.add(occur);
				TFs.add(df.format(TF));
			}

			if (lineNumber % 10000 == 0) {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
				System.out.print(sdf.format(cal.getTime()));
				System.out.println(" => Status at: " + lineNumber + " loss: " + (num * 100 / den));
			}

			if (pageIds.isEmpty())
				continue;

			wr.write(keyword);
			wr.write("\t" + StringUtils.join(pageIds, ';'));
			wr.write("\t" + StringUtils.join(numOfOccurence, ';'));
			wr.write("\t" + StringUtils.join(TFs, ';'));
			wr.write("\t" + df.format(IDF) + "\n");

		}
		wr.close();
		in.close();
	}

	/**
	 * The function is responsible for parsing the generated output file from the
	 * Hadoop page length collection stream run, and converting it into a file
	 * ingestable into the pageDetails table
	 * 
	 * @param filePath
	 *            The path to the generated page length file from Hadoop stream run
	 * @param titlePath
	 *            The path to a csv file which stores the title name along with its
	 *            respective id as in the enwiki xml dataset e.g: 1,Asia 2,Sun Burn
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void updateStream(final String filePath, final String titlePath)
			throws SQLException, IOException {
		Scanner in = new Scanner(new File(filePath));
		final Map<String, Integer> titleToPageLengthMap = new HashMap<String, Integer>();

		System.out.println("..Caching..");
		int lineNum = 0;
		while (in.hasNext()) {
			try {
				final String title = TokenHelper.processTitle(in.next());
				final int streamLength = in.nextInt();

				lineNum++;
				titleToPageLengthMap.put(title, streamLength);
			} catch (Exception ex) {
				System.out.println("ERR AT: " + lineNum);
				ex.printStackTrace();
			}
		}

		System.out.println("..Completed..");

		CSVReader reader = new CSVReader(new FileReader(titlePath));
		Scanner fin = new Scanner(new File(titlePath));

		String[] nextLine;

		FileWriter writer = new FileWriter(ARTIFACTS_DIR + "withLength.csv");
		while ((nextLine = reader.readNext()) != null) {
			try {
				// final int id = Integer.parseInt(nextLine[0]);
				final String title = nextLine[1];
				final String line = fin.nextLine();

				if (!titleToPageLengthMap.containsKey(title))
					continue;
				writer.write(line + ",\"" + titleToPageLengthMap.get(title) + "\"\n");
			} catch (Exception ex) {
				System.out.println(nextLine);
				ex.printStackTrace();
			}
		}
		reader.close();
		writer.close();
		fin.close();
		in.close();
	}

	public static void main(String[] args) throws Exception {
		parseFile(ARTIFACTS_DIR + "merged-index-file", ARTIFACTS_DIR + "titles_with_id.csv");
	}
}
