package org.kernelab.utils.regex;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;

import org.kernelab.basis.Tools;
import org.kernelab.basis.Variable;
import org.kernelab.basis.io.CharsTransfer;
import org.kernelab.basis.io.StringBuilderWriter;

public class Regex
{
	/**
	 * regex: the regular expression<br />
	 * flags: combination of musicx<br />
	 * split: two hex-char such as 0A<br />
	 * replace: $ as the back-reference
	 * 
	 * @param args
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

				CharSequence result = match(writer.getBuilder(), regex, flags, split, replace);

				if (result != null)
				{
					System.out.print(result);
				}
			}
		}
	}

	public static CharSequence match(CharSequence text, String regex, String flags, String split, String replace)
	{
		boolean global = flags != null && flags.indexOf('g') != -1;

		Matcher matcher = Tools.matcher(regex, flags, text);

		if (replace != null)
		{
			boolean extract = flags != null && flags.indexOf('x') != -1;

			if (extract)
			{
				if (global)
				{
					StringBuilder buffer = null;

					while (matcher.find())
					{
						if (buffer == null)
						{
							buffer = new StringBuilder(text.length() / 10);
						}
						buffer.append(matcher.group().replaceFirst(regex, replace));
						buffer.append(split);
					}

					return buffer;
				}
				else
				{
					if (matcher.find())
					{
						return matcher.group().replaceFirst(regex, replace);
					}
					else
					{
						return null;
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
					}
					matcher.appendTail(buffer);

					return buffer;
				}
				else
				{
					return matcher.replaceFirst(replace);
				}
			}
		}
		else
		{
			if (global)
			{
				StringBuilder buffer = null;

				while (matcher.find())
				{
					if (buffer == null)
					{
						buffer = new StringBuilder();
					}
					buffer.append(matcher.group());
					buffer.append(split);
				}

				return buffer;
			}
			else
			{
				if (matcher.find())
				{
					return matcher.group();
				}
				else
				{
					return null;
				}
			}
		}
	}
}
