package org.kernelab.utils.regex;

import java.io.File;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.Canal;
import org.kernelab.basis.Canal.Tuple;
import org.kernelab.basis.Canal.Tuple2;
import org.kernelab.basis.Entrance;
import org.kernelab.basis.Filter;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;
import org.kernelab.basis.io.TextDataSource;

public class BatchReplacer
{
	public static void main(String[] args)
	{
		Entrance entr = new Entrance().handle(args);
		BatchReplacer r = new BatchReplacer();
		r.setSource(new File(entr.parameter("src"))) //
				.setMapper(new File(entr.parameter("map"))) //
				.setTarget(new File(entr.parameter("dst"))) //
				.process();
	}

	private File	source;

	private File	mapper;

	private File	target;

	public File getMapper()
	{
		return mapper;
	}

	public File getSource()
	{
		return source;
	}

	public File getTarget()
	{
		return target;
	}

	protected Iterable<Tuple2<Pattern, String>> iterateMapper(File file)
	{
		return Canal.of(new TextDataSource(file, Charset.forName("UTF-8"), "\n")).map(new Mapper<String, String>()
		{
			@Override
			public String map(String el) throws Exception
			{
				return el.trim();
			}
		}).filter(new Filter<String>()
		{
			@Override
			public boolean filter(String el) throws Exception
			{
				return !el.isEmpty();
			}
		}).map(new Mapper<String, Tuple2<Pattern, String>>()
		{
			@Override
			public Tuple2<Pattern, String> map(String el) throws Exception
			{
				String delimit = String.valueOf(el.charAt(0));

				String[] arr = el.split(Pattern.quote(delimit), -1);

				if (arr.length < 3)
				{
					return null;
				}

				return Tuple.of(Pattern.compile(arr[1]), arr[2]);
			}
		}).filter(new Filter<Tuple2<Pattern, String>>()
		{
			@Override
			public boolean filter(Tuple2<Pattern, String> el) throws Exception
			{
				return el != null;
			}
		});
	}

	public BatchReplacer process()
	{
		CharSequence text = readSource(this.getSource());

		for (Tuple2<Pattern, String> pair : this.iterateMapper(this.getMapper()))
		{
			text = replace(text, pair._1, pair._2);
		}

		writeResult(text, this.getTarget());

		return this;
	}

	protected CharSequence readSource(File file)
	{
		return Tools.inputStringFromFile(file, "UTF-8");
	}

	protected CharSequence replace(CharSequence s, Pattern p, String r)
	{
		Matcher m = p.matcher(s);

		StringBuffer sb = new StringBuffer();

		while (m.find())
		{
			m.appendReplacement(sb, r);
		}
		m.appendTail(sb);

		return sb;
	}

	public BatchReplacer setMapper(File mapper)
	{
		this.mapper = mapper;
		return this;
	}

	public BatchReplacer setSource(File source)
	{
		this.source = source;
		return this;
	}

	public BatchReplacer setTarget(File target)
	{
		this.target = target;
		return this;
	}

	protected void writeResult(CharSequence text, File target)
	{
		Tools.outputStringToFile(target, text.toString(), "UTF-8", false);
	}
}
