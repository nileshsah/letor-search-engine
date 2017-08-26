package com.nellex.hadoop.pageLength;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.wikiclean.WikiClean;
import org.wikiclean.WikiClean.WikiLanguage;
import org.wikiclean.WikiCleanBuilder;

import com.nellex.helpers.TokenHelper;

public class WikiStreamMapper extends Mapper<LongWritable, Text, Text, LongWritable> {

	@Override
	public void map(LongWritable key, Text value, Context context) throws IOException,
			InterruptedException {

		// Returns String[0] = <title>[TITLE]</title>
		// String[1] = <text>[CONTENT]</text>
		// !! without the <tags>.
		String[] titleAndText = parseTitleAndText(value);

		String pageString = titleAndText[0];
		if (notValidPage(pageString))
			return;

		String page = TokenHelper.processTitle(pageString);

		WikiClean cleaner = new WikiCleanBuilder().withLanguage(WikiLanguage.EN).withTitle(false)
				.withFooter(false).build();
		String content = cleaner.clean(value.toString());

		String[] tokens = content.split(" ");

		context.write(new Text(page), new LongWritable(tokens.length));
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
