package com.nellex.api;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nellex.experimental.LocalSearchEngine;
import com.nellex.helpers.FeatureExtractor;

import ciir.umass.edu.eval.Evaluator;

public class SearchServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setStatus(HttpStatus.OK_200);
		try {
			resp.getWriter()
					.println("controller.processData(" + search(req.getParameter("query")) + ")");
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private String search(String name) throws IOException, SQLException, JSONException {
		final String str = name;
		JSONArray jsonArr = new JSONArray();

		File testFile = File.createTempFile("test-", ".tmp");
		FileWriter writer = new FileWriter(testFile);
		final String row = FeatureExtractor.getFeatureSet(str, null);
		writer.write(row);
		writer.close();

		File tmpFile = File.createTempFile("pred-", ".tmp");
		Map<Integer, String> qRow = LocalSearchEngine.parseFeature(row);

		final String arg = "-load " + RANKING_MODEL + " -rank " + testFile.getAbsolutePath()
				+ " -score " + tmpFile.getAbsolutePath();
		Evaluator.main(arg.split(" "));

		Scanner predFile = new Scanner(tmpFile);
		List<Pair<Double, Integer>> result = new ArrayList<Pair<Double, Integer>>();

		Map<Integer, String> tMap = new HashMap<Integer, String>();

		while (predFile.hasNextLine()) {
			final String line = predFile.nextLine();
			final String[] params = line.split("\t");

			final double score = Double.parseDouble(params[2]);
			final int id = Integer.parseInt(params[1]);

			final String title = qRow.get(id).split("#")[1];
			tMap.put(id, title);

			result.add(new ImmutablePair<Double, Integer>(score, id));
		}

		Collections.sort(result);
		Collections.reverse(result);

		int idx = 1;

		for (Pair<Double, Integer> pair : result) {
			String title = tMap.get(pair.getRight());
			String weblink = "https://en.wikipedia.org/wiki/" + title;
			String titleP = title.replace("_", " ");

			JSONObject jsonObj = new JSONObject();
			jsonObj.put("index", idx);
			jsonObj.put("score", pair.getLeft());
			jsonObj.put("title", titleP);
			jsonObj.put("weblink", weblink);
			jsonObj.put("extract", qRow.get(pair.getRight()));

			jsonArr.put(jsonObj);
			idx += 1;
		}
		predFile.close();
		return jsonArr.toString();
	}
}
