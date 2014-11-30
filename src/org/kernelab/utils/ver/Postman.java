package org.kernelab.utils.ver;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.kernelab.basis.JSON;
import org.kernelab.basis.Tools;

public class Postman
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		JSON map = JSON.Parse(new InputStreamReader(Tools.getClassLoader().getResourceAsStream(
				"org/kernelab/utils/ver/map.json")));

		Postman postman = new Postman().setMap(map);

		if (args == null || args.length == 0)
		{
			JFileChooser fc = new JFileChooser(".");
			fc.setMultiSelectionEnabled(false);

			if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				try
				{
					postman.deliver(fc.getSelectedFile());
					JOptionPane.showMessageDialog(null, fc.getSelectedFile().getName() + " delivered", "OK",
							JOptionPane.INFORMATION_MESSAGE);
				}
				catch (IOException e)
				{
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), e.getClass().getName(),
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		else
		{
			try
			{
				postman.deliver(new File(args[0]));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private JSON	map;

	public Postman()
	{

	}

	public void deliver(File file) throws IOException
	{
		if (file != null && file.isFile())
		{
			String filepath = file.getCanonicalPath();

			for (String path : map.valJSAN(file.getName(), true).iterator(String.class))
			{
				Tools.debug(filepath + " to " + path);
				Tools.copy(file, new File(path));
			}

			Tools.debug(file.getCanonicalPath() + " delivered");
		}
	}

	public JSON getMap()
	{
		return map;
	}

	public Postman setMap(JSON map)
	{
		this.map = map;
		return this;
	}
}
