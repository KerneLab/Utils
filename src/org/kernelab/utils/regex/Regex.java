package org.kernelab.utils.regex;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Matcher;

import org.kernelab.basis.Tools;
import org.kernelab.basis.Variable;
import org.kernelab.basis.io.CharsTransfer;
import org.kernelab.basis.io.StringBuilderWriter;

public class Regex
{
	/**
	 * regex: the regular expression<br />
	 * flags: combination of lodmusixr<br />
	 * split: two hex-char such as 0A<br />
	 * replace: $ as the back-reference
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args)
	{
		if (args != null && args.length >= 2)
		{
			String regex = args[0], flags = args[1], split = null, replace = null;

			if (regex != null)
			{
				if (args.length > 2)
				{
					split = args[2];
				}

				if (split == null)
				{
					split = System.getProperty("line.separator");
				}
				else
				{
					StringBuilder buf = new StringBuilder();

					for (byte b : Tools.dumpBytes(Tools.splitCharSequence(split, 2)))
					{
						buf.append((char) b);
					}

					split = buf.toString();
				}

				split = split == null ? "" : split;

				if (args.length > 3)
				{
					replace = args[3];
				}

				int size = Variable.asInteger(System.getProperty("init.buff.size"), 10000);

				Reader reader = new InputStreamReader(System.in);

				StringBuilderWriter writer = new StringBuilderWriter(size);

				new CharsTransfer(reader, writer).run();

				try
				{
					reader.close();
				}
				catch (IOException e)
				{
				}

				PrintWriter out = new PrintWriter(System.out);

				try
				{
					match(out, writer.getBuilder(), regex, flags, split, replace);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				out.flush();
			}
		}
	}

	public static void match(Writer out, CharSequence text, String regex, String flags, String split, String replace)
			throws IOException
	{
		boolean global = flags != null && flags.indexOf('g') != -1;

		Matcher matcher = Tools.matcher(regex, flags, text);

		if (replace != null)
		{
			boolean extract = flags != null && flags.indexOf('r') != -1;

			if (extract)
			{
				if (global)
				{
					while (matcher.find())
					{
						out.write(matcher.group().replaceFirst(regex, replace));
						out.write(split);
					}
				}
				else
				{
					if (matcher.find())
					{
						out.write(matcher.group().replaceFirst(regex, replace));
					}
				}
			}
			else
			{
				if (global)
				{
					StringBuffer buffer = new StringBuffer(text.length());

					while (matcher.find())
					{
						matcher.appendReplacement(buffer, replace);
						out.write(buffer.toString());
						buffer.delete(0, buffer.length());
					}

					matcher.appendTail(buffer);
					out.write(buffer.toString());
				}
				else
				{
					out.write(matcher.replaceFirst(replace));
				}
			}
		}
		else
		{
			if (global)
			{
				while (matcher.find())
				{
					out.write(matcher.group());
					out.write(split);
				}
			}
			else
			{
				if (matcher.find())
				{
					out.write(matcher.group());
				}
			}
		}
	}
}
