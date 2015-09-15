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
	public static void main(String[] args)
	{
		if (args != null && args.length >= 2)
		{
			String regex = args[0], flags = args[1], replace = null;

			if (regex != null)
			{
				if (args.length > 2)
				{
					replace = args[2];
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

				CharSequence result = match(writer.getBuilder(), regex, flags, replace);

				if (result != null)
				{
					System.out.print(result);
				}
			}
		}
	}

	public static CharSequence match(CharSequence text, String regex, String flags, String replace)
	{
		boolean global = flags != null && flags.indexOf('g') != -1;

		Matcher matcher = Tools.matcher(regex, flags, text);

		if (replace != null)
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
		else
		{
			if (global)
			{
				StringBuilder buffer = null;

				String ls = System.getProperty("line.separator");

				while (matcher.find())
				{
					if (buffer == null)
					{
						buffer = new StringBuilder();
					}
					buffer.append(matcher.group());
					buffer.append(ls);
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
