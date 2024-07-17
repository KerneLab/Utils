package org.kernelab.utils.regex;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Tools;

public class AppBatchReplacer
{
	public static void main(String[] args)
	{
		try
		{
			JFileChooser fc = new JFileChooser(".");
			fc.setMultiSelectionEnabled(false);
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

			fc.setDialogTitle("Open Mapper File ...");
			if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				File mapper = fc.getSelectedFile();

				fc.setCurrentDirectory(mapper.getParentFile());
				fc.setDialogTitle("Open Source File ...");
				if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
				{
					File source = fc.getSelectedFile();

					fc.setCurrentDirectory(source.getParentFile());
					fc.setDialogTitle("Open Target File ...");
					if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
					{
						File target = fc.getSelectedFile();

						if (target.equals(mapper))
						{
							throw new IllegalArgumentException("target file can not be the same as mapper file.");
						}
						if (target.equals(source))
						{
							throw new IllegalArgumentException("target file can not be the same as source file.");
						}

						JSAN params = new JSAN() //
								.addLast("-map", mapper.getAbsolutePath(), //
										"-src", source.getAbsolutePath(), //
										"-dst", target.getAbsolutePath() //
								);

						BatchReplacer.main(params.toArray(new String[params.size()]));

						JOptionPane.showMessageDialog(null, "Output to " + Tools.getFilePath(target), "Done",
								JOptionPane.INFORMATION_MESSAGE);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();

			StringBuilder buffer = new StringBuilder(e.getLocalizedMessage());
			for (StackTraceElement trace : e.getStackTrace())
			{
				buffer.append("\n ");
				buffer.append(trace.toString());
			}

			JOptionPane.showMessageDialog(null, buffer.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
