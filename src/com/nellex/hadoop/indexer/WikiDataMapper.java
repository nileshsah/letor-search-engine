package com.nellex.hadoop.indexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharacterCodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.wikiclean.WikiClean;
import org.wikiclean.WikiClean.WikiLanguage;
import org.wikiclean.WikiCleanBuilder;

import com.nellex.helpers.TokenHelper;

public class WikiDataMapper extends Mapper<LongWritable, Text, Text, Text> {

	static HashMap<String, Integer> stopWords = new HashMap<String, Integer>();

	private void setup() throws IOException {
		if (WikiDataMapper.stopWords != null)
			return;
		final Path pathToFile = new Path("s3://nxwikibucket/stop_words.txt");
		// final Path pathToFile = new Path("stop_words.txt");

		final FileSystem fileSystem = FileSystem.get(new Configuration());

		final Path hdfsPath = fileSystem.resolvePath(pathToFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fileSystem.open(hdfsPath)));
		String line;
		while ((line = br.readLine()) != null) {
			WikiDataMapper.stopWords.put(line, 1);
		}
	}

	@Override
	public void map(LongWritable key, Text value, Context context) throws IOException,
			InterruptedException {
		setup();
		// Returns String[0] = <title>[TITLE]</title>
		// String[1] = <text>[CONTENT]</text>
		// !! without the <tags>.

		if (value.toString().contains("\"preserve\">#REDIRECT [["))
			return;

		String[] titleAndText = parseTitleAndText(value);

		String pageString = titleAndText[0];
		if (notValidPage(pageString))
			return;

		String page = TokenHelper.processTitle(pageString);

		WikiClean cleaner = new WikiCleanBuilder().withLanguage(WikiLanguage.EN).withTitle(false)
				.withFooter(false).build();
		String content = cleaner.clean(value.toString());

		String[] tokens = content.split(" ");

		page += "|" + tokens.length;
		Map<String, Integer> wordCount = new HashMap<String, Integer>();

		for (String token : tokens) {
			String word = TokenHelper.processToken(token, false);
			word = TokenHelper.processToken(word, true);

			if (word.isEmpty() || stopWords.containsKey(word) || !StringUtils.isAlpha(word))
				continue;
			if (!wordCount.containsKey(word))
				wordCount.put(word, 0);
			wordCount.put(word, wordCount.get(word) + 1);
		}

		for (String wordToken : wordCount.keySet()) {
			final String pageParam = page + "|" + wordCount.get(wordToken);
			context.write(new Text(wordToken), new Text(pageParam));
		}
	}

	private boolean notValidPage(String pageString) {
		return pageString.contains(":");
	}

	private String[] parseTitleAndText(Text value) throws CharacterCodingException {
		String[] titleAndText = new String[2];

		int start = value.find("<title>");
		int end = value.find("</title>", start);
		start += 7; // add <title> length.

		titleAndText[0] = Text.decode(value.getBytes(), start, end - start);

		start = value.find("<text");
		start = value.find(">", start);
		end = value.find("</text>", start);
		start += 1;

		if (start == -1 || end == -1) {
			return new String[] { "", "" };
		}

		titleAndText[1] = Text.decode(value.getBytes(), start, end - start);

		return titleAndText;
	}
}
