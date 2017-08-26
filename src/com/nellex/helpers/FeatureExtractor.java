package com.nellex.helpers;

import static com.nellex.helpers.DataCommon.ARTIFACTS_DIR;
import static com.nellex.helpers.DataCommon.STOP_WORDS;
import static com.nellex.helpers.DataCommon.TITLES_TO_ID_FILE_PATH;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Class responsible for extracting required features from different pages for
 * the given search query as collected for the training and testing data set
 * 
 * @author nellex
 */
public class FeatureExtractor {
	private final static Map<Integer, Map<String, Object>> pageDetails = new HashMap<Integer, Map<String, Object>>();

	private static Map<String, Integer> titleMap = null;

	private static JSONObject parseJson(final String filePath) throws IOException, ParseException {
		JSONParser parser = new JSONParser();
		final String json = new String(Files.readAllBytes(Paths.get(filePath)));

		return (JSONObject) parser.parse(json);
	}

	private static List<String> getPageTitles(final JSONObject jsonObj) {
		final List<String> results = new ArrayList<String>();
		JSONArray titles = (JSONArray) ((JSONObject) jsonObj.get("query")).get("search");
		for (int i = 0; i < titles.size(); i++) {
			JSONObject titleObj = (JSONObject) titles.get(i);
			final String title = (String) titleObj.get("title");
			results.add(TokenHelper.processTitle(title));
		}
		return results;
	}

	private static String getQuery(final JSONObject jsonObj) {
		return (String) jsonObj.get("queryText");
	}

	private static class QueryResult {
		public String title;
		public int id;
		public int pos;
	};

	private static int getLabel(final int key) {
		return 6 - (((key - 1) / 6) + 1);
	}

	private static int qid = 1;

	private static Map<String, Double> createMap() {
		HashMap<String, Double> feature = new HashMap<String, Double>();

		feature.put("query_id", 0.0);
		feature.put("result_id", 0.0);
		feature.put("query_length", 0.0);

		feature.put("covered_query_number", 0.0);
		feature.put("title_length", 0.0);
		feature.put("covered_query_ratio", 0.0);

		feature.put("stream_length", 0.0);
		feature.put("page_rank", 0.0);

		feature.put("freq_sum", 0.0);
		feature.put("freq_min", 0.0);
		feature.put("freq_max", 0.0);
		feature.put("freq_avg", 0.0);

		feature.put("TF_sum", 0.0);
		feature.put("TF_min", 0.0);
		feature.put("TF_max", 0.0);
		feature.put("TF_avg", 0.0);

		feature.put("TFIDF_sum", 0.0);
		feature.put("TFIDF_min", 0.0);
		feature.put("TFIDF_max", 0.0);
		feature.put("TFIDF_avg", 0.0);

		feature.put("LABEL", 0.0);

		return feature;
	}

	/*
	 * Takes as input the search query and the titles where the terms exists and
	 * returns an hash map of features for all the titles
	 */
	private static Map<String, Map<String, Double>> processQuery(final String queryText,
			final Map<Integer, QueryResult> toProcess) throws SQLException {
		Connection conn = DatabaseConnector.connectDB();

		if (pageDetails.size() > 1000000)
			pageDetails.clear();

		List<String> QList = new ArrayList<String>();
		Map<Integer, Map<String, Double>> QDP = new HashMap<Integer, Map<String, Double>>();
		Map<String, Map<String, Double>> pages = new HashMap<String, Map<String, Double>>();

		List<Double> heuristic = new ArrayList<Double>();

		for (String str : queryText.split(" "))
			QList.add(TokenHelper.processToken(str, true));

		String IN = "(";
		boolean isFirst = true;

		Map<String, Integer> isPresent = new HashMap<String, Integer>();
		for (String stemmed : QList) {
			if (isPresent.containsKey(stemmed))
				continue;
			if (!isFirst)
				IN += ",";
			IN += "'" + stemmed + "'";
			isPresent.put(stemmed, 1);
			isFirst = false;
		}
		IN += ")";

		System.out.println(IN);

		Statement forPageIndex = conn.createStatement();
		ResultSet rs = forPageIndex.executeQuery("SELECT * FROM pageIndex WHERE token IN " + IN);

		String[] strPageId, strTF, strNumOfOccurence;
		Map<Integer, Integer> hits = new HashMap<Integer, Integer>();

		while (rs.next()) {
			System.out.println(rs.getString("token"));
			strPageId = rs.getString("pageId").split(";");
			strTF = rs.getString("TF").split(";");
			strNumOfOccurence = rs.getString("numOfOccurence").split(";");
			final double IDF = rs.getDouble("IDF");

			Map<Integer, Integer> mapOfOccurence = new HashMap<Integer, Integer>();
			Map<Integer, Double> mapOfTF = new HashMap<Integer, Double>();

			StringBuilder sb = new StringBuilder();
			isFirst = true;
			for (int i = 0; i < strPageId.length; i++) {
				Integer id = Integer.parseInt(strPageId[i]);
				mapOfOccurence.put(id, Integer.parseInt(strNumOfOccurence[i]));
				mapOfTF.put(id, Double.parseDouble(strTF[i]));
				heuristic.add(
						Double.parseDouble(strNumOfOccurence[i]) + Double.parseDouble(strTF[i]));
				if (pageDetails.containsKey(id))
					continue;
				if (!isFirst)
					sb.append(",");
				sb.append(strPageId[i]);
				isFirst = false;
			}

			if (sb.toString().isEmpty())
				return pages;

			System.out.println("Fecthing features from db ..");

			Collections.sort(heuristic);
			Collections.reverse(heuristic);

			final double theta = heuristic.get(Math.min(heuristic.size() - 1, 1000));

			Statement forPageDetails = conn.createStatement();
			ResultSet iterator = forPageDetails
					.executeQuery("SELECT * FROM pageDetails WHERE id IN (" + sb.toString() + ")");
			ResultSetMetaData md = iterator.getMetaData();
			int columns = md.getColumnCount();

			while (iterator.next()) {
				Map<String, Object> row = new HashMap<String, Object>(columns);
				for (int i = 1; i <= columns; i++)
					row.put(md.getColumnName(i), iterator.getObject(i));
				pageDetails.put((Integer) row.get("id"), row);
			}

			System.out.println("Fetching complete.." + pageDetails.size());

			for (String pId : strPageId) {
				final Integer id = Integer.parseInt(pId);
				if (!pageDetails.containsKey(id)) {
					continue;
				}

				if (toProcess != null && !toProcess.containsKey(id))
					continue;

				final double hval = (double) mapOfOccurence.get(id) + mapOfTF.get(id);
				if (hval < theta)
					continue;

				final String title = pageDetails.get(id).get("pageTitle").toString();
				Map<String, Double> feature;

				if (!hits.containsKey(id))
					hits.put(id, 0);
				hits.put(id, hits.get(id) + 1);

				if (!QDP.containsKey(id)) {
					feature = createMap();

					feature.put("query_id", (double) qid);
					feature.put("result_id", (double) id);

					String[] token = (title + "_").split("_");
					double matches = 0;
					for (String s : token) {
						final String stemmedTitle = TokenHelper.processToken(s, true);
						if (QList.contains(stemmedTitle))
							matches++;
					}

					feature.put("covered_query_number", matches);
					feature.put("title_length", (double) token.length);
					feature.put("covered_query_ratio", matches / token.length);

					feature.put("stream_length",
							((Integer) pageDetails.get(id).get("pageLength")).doubleValue());
					feature.put("page_rank", (Double) pageDetails.get(id).get("pageRank"));

					feature.put("freq_min", (double) mapOfOccurence.get(id));
					feature.put("TF_min", (double) mapOfTF.get(id));
					feature.put("TFIDF_min", (double) mapOfTF.get(id) * IDF);
				} else
					feature = QDP.get(id);

				double freq = mapOfOccurence.get(id);
				feature.put("freq_sum", feature.get("freq_sum") + freq);
				feature.put("freq_min", Math.min(feature.get("freq_min"), freq));
				feature.put("freq_max", Math.max(feature.get("freq_max"), freq));
				feature.put("freq_avg", feature.get("freq_avg") + 1.0 * freq / QList.size());

				double TF = mapOfTF.get(id);
				feature.put("TF_sum", feature.get("TF_sum") + TF);
				feature.put("TF_min", Math.min(feature.get("TF_min"), TF));
				feature.put("TF_max", Math.max(feature.get("TF_max"), TF));
				feature.put("TF_avg", feature.get("TF_avg") + 1.0 * TF / QList.size());

				double TFIDF = mapOfTF.get(id) * IDF;
				feature.put("TFIDF_sum", feature.get("TFIDF_sum") + TFIDF);
				feature.put("TFIDF_min", Math.min(feature.get("TFIDF_min"), TFIDF));
				feature.put("TFIDF_max", Math.max(feature.get("TFIDF_max"), TFIDF));
				feature.put("TFIDF_avg", feature.get("TFIDF_avg") + 1.0 * TFIDF / QList.size());

				QDP.put(id, feature);
			}
		}

		if (QDP.isEmpty()) {
			System.out.println("Empty results for " + queryText);
			return pages;
		}

		for (Integer key : QDP.keySet()) {
			Map<String, Double> feature = QDP.get(key);
			if (hits.get(key) < QList.size()) {
				feature.put("TF_min", 0.0);
				feature.put("TFIDF_min", 0.0);
				feature.put("freq_min", 0.0);
			}

			final String title = (String) pageDetails.get(key).get("pageTitle");
			pages.put(title, QDP.get(key));
		}

		System.out.println("Fetching features complete at " + QDP.size());
		++qid;
		return pages;
	}

	private static Map<Integer, QueryResult> getQueryList(JSONObject jsonObj) throws Exception {

		if (titleMap == null)
			titleMap = IndexFileParser.getTitleMap(TITLES_TO_ID_FILE_PATH);

		List<String> titles = getPageTitles(jsonObj);
		Map<Integer, QueryResult> titlesToProcess = new HashMap<Integer, QueryResult>();

		int missed = 0, ptr = 0;
		for (String str : titles) {
			ptr++;
			if (ptr > 30)
				break;
			if (titleMap.containsKey(str)) {
				QueryResult qr = new QueryResult();
				qr.title = str;
				qr.pos = ptr;
				qr.id = titleMap.get(str);
				titlesToProcess.put(qr.id, qr);
			} else {
				missed++;
				System.out.println("Missed: " + str + " => " + ptr);
			}
		}

		System.out.println(missed + " of " + titles.size() + " => " + 1.0 * missed / titles.size());

		return titlesToProcess;
	}

	private static String fetchRow(Map<String, Double> feature) {
		DecimalFormat df = new DecimalFormat("#.######");
		df.setRoundingMode(RoundingMode.CEILING);

		final int label = (int) Math.round(feature.get("LABEL"));
		feature.remove("LABEL");

		final int qid = (int) Math.round(feature.get("query_id"));
		feature.remove("query_id");

		String row = "";
		row += label + " ";
		row += "qid:" + qid;

		int idx = 1;
		for (String s : feature.keySet()) {
			row += " " + idx + ":" + df.format(feature.get(s));
			++idx;
		}
		return row;
	}

	/**
	 * For a given search query, returns all the pages where the search query is
	 * relevant along with the extracted features of that page as a CSV
	 * 
	 * @param queryText
	 *            The string to search for
	 * @param titles
	 *            Optional map of titles and their position in the search results,
	 *            used during training purpose to demarcate labels for the training
	 *            set
	 * @throws SQLException
	 */
	public static String getFeatureSet(final String queryText,
			final Map<Integer, QueryResult> titles) throws SQLException {
		Map<String, Map<String, Double>> featureSet = processQuery(queryText, titles);
		String row = "\n#" + queryText;
		for (String title : featureSet.keySet()) {
			Map<String, Double> feature = featureSet.get(title);
			final int id = feature.get("result_id").intValue();
			if (titles != null)
				feature.put("LABEL", (double) getLabel(titles.get(id).pos));
			else
				feature.put("LABEL", (double) 1.0);
			feature.remove("result_id");
			row += '\n' + fetchRow(feature) + " #" + title;
		}
		return row;
	}

	private static void run(final String filePath, final String outPath) throws Exception {
		JSONObject jsonObj = parseJson(filePath);
		FileWriter writer = new FileWriter(outPath, true);

		final Map<Integer, QueryResult> titlesToProcess = getQueryList(jsonObj);
		final String queryText = getQuery(jsonObj).replace("+", " ");

		final String row = getFeatureSet(queryText, titlesToProcess);
		writer.write(row);
		writer.close();
	}

	private static void generateData(final String dirPath, final String outPath) throws Exception {
		File dir = new File(dirPath);
		File[] files = dir.listFiles();

		Map<String, Integer> stopWords = new HashMap<String, Integer>();
		Scanner in = new Scanner(new File(STOP_WORDS));
		while (in.hasNextLine())
			stopWords.put(in.nextLine(), 1);

		for (File child : files) {
			boolean canProcess = true;
			for (String str : child.getName().split("\\+"))
				if (stopWords.containsKey(str))
					canProcess = false;
			if (canProcess)
				run(child.getAbsolutePath(), outPath);
		}
		in.close();
	}

	public static void main(String[] args) throws Exception {
		generateData(ARTIFACTS_DIR + "mix-data/", ARTIFACTS_DIR + "train_mix.dat");
		// processQuery("custom", null);
	}
}
