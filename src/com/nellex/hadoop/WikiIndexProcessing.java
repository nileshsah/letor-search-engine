package com.nellex.hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.util.ConfigUtil;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.nellex.hadoop.indexer.WikiDataMapper;
import com.nellex.hadoop.indexer.WikiDataReducer;
import com.nellex.pageRank.hadoop.job1.xmlhakker.XmlInputFormat;

public class WikiIndexProcessing extends ConfigUtil implements Tool {

	public static void main(String[] args) {
		try {
			System.exit(ToolRunner.run(new Configuration(), new WikiIndexProcessing(), args));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int run(String[] args) throws Exception {
		boolean isCompleted = runXmlParsing(args[1], args[2]);
		if (!isCompleted)
			return 1;
		return 0;
	}

	public boolean runXmlParsing(String inputPath, String outputPath)
			throws IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();
		conf.set(XmlInputFormat.START_TAG_KEY, "<page>");
		conf.set(XmlInputFormat.END_TAG_KEY, "</page>");
		conf.setLong("mapred.task.timeout", 1800000);
		conf.setLong("mapreduce.task.timeout", 1800000);

		Job xmlParser = Job.getInstance(conf, "xmlParser");
		xmlParser.setJarByClass(WikiIndexProcessing.class);

		// Input / Mapper
		FileInputFormat.addInputPath(xmlParser, new Path(inputPath));
		xmlParser.setInputFormatClass(XmlInputFormat.class);
		xmlParser.setMapperClass(WikiDataMapper.class);
		xmlParser.setMapOutputKeyClass(Text.class);

		// Output / Reducer
		FileOutputFormat.setOutputPath(xmlParser, new Path(outputPath));
		xmlParser.setOutputFormatClass(TextOutputFormat.class);
		xmlParser.setOutputKeyClass(Text.class);
		xmlParser.setOutputValueClass(Text.class);
		xmlParser.setReducerClass(WikiDataReducer.class);

		return xmlParser.waitForCompletion(true);
	}

	@Override
	public Configuration getConf() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setConf(Configuration arg0) {
		// TODO Auto-generated method stub

	}

}
