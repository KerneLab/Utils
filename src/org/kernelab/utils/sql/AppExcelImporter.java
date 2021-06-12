package org.kernelab.utils.sql;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.DataBase;
import org.kernelab.basis.sql.DataBase.GeneralDataBase;
import org.kernelab.basis.sql.SQLKit;
import org.kernelab.utils.sql.ExcelImporter.ImportListener;

public class AppExcelImporter extends JFrame implements ImportListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4437119848040215123L;

	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		AppExcelImporter app = new AppExcelImporter();
		app.showApp();
	}

	private ExcelImporter	importer;

	private File			openDir	= new File("./");

	private JTextField		jdbcDbText;

	private JTextField		jdbcUsrText;

	private JPasswordField	jdbcPwdText;

	private JButton			jdbcLinkButton;

	private JButton			openFileButton;

	private JLabel			sheetIndexLabel;

	private JLabel			filePathLabel;

	private JTextField		tableNameText;

	private JButton			createTableButton;

	private JButton			cleanTableButon;

	private JButton			insertDataButton;

	private JProgressBar	progressBar;

	public AppExcelImporter()
	{
		super();

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.config();

		this.arrange();
	}

	public void arrange()
	{
		JPanel panel = new JPanel();

		panel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = Tools.makePreferredGridBagConstraints();
		gbc.insets = new Insets(2, 4, 2, 4);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		panel.add(new JLabel("数据库"), gbc);

		gbc.gridx++;
		gbc.weightx = 1;
		panel.add(this.jdbcDbText, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 0;
		panel.add(new JLabel("用户名"), gbc);

		gbc.gridx++;
		gbc.weightx = 1;
		panel.add(this.jdbcUsrText, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 0;
		panel.add(new JLabel("密码"), gbc);

		gbc.gridx++;
		gbc.weightx = 1;
		panel.add(this.jdbcPwdText, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		panel.add(this.jdbcLinkButton, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		panel.add(this.openFileButton, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		panel.add(this.sheetIndexLabel, gbc);

		gbc.gridx++;
		gbc.weightx = 1;
		panel.add(this.filePathLabel, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		panel.add(new JLabel("表名"), gbc);

		gbc.gridx++;
		gbc.weightx = 1;
		panel.add(this.tableNameText, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		panel.add(this.createTableButton, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		panel.add(this.cleanTableButon, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		panel.add(this.insertDataButton, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		panel.add(this.progressBar, gbc);

		//////////////////////////////////////////////

		this.setLayout(new GridBagLayout());
		gbc = Tools.makePreferredGridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		this.add(panel, gbc);

		this.pack();

		Dimension size = this.getSize();
		this.setMinimumSize(size);
	}

	public void config()
	{
		this.importer = new ExcelImporter();
		this.importer.addListener(this);

		this.jdbcDbText = new JTextField(30);

		this.jdbcUsrText = new JTextField(30);

		this.jdbcPwdText = new JPasswordField(30);

		this.jdbcLinkButton = new JButton("连接数据库");
		this.jdbcLinkButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doLinkDataBase();
			}
		});

		this.openFileButton = new JButton("打开Excel");
		this.openFileButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doOpenExcelFile();
			}
		});

		this.sheetIndexLabel = new JLabel(" ");

		this.filePathLabel = new JLabel(" ");

		this.tableNameText = new JTextField(30);

		this.createTableButton = new JButton("建表");
		this.createTableButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doCreateTable();
			}
		});

		this.cleanTableButon = new JButton("清表");
		this.cleanTableButon.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doCleanTable();
			}
		});

		this.insertDataButton = new JButton("导入");
		this.insertDataButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doInsertData();
			}
		});

		this.progressBar = new JProgressBar();

		this.setTitle("Excel Importer");
	}

	protected void doCleanTable()
	{
		try
		{
			this.getImporter().setTable(this.getTableName()).doClean();
			hint("已成功清理数据表" + this.getTableName());
		}
		catch (Exception e)
		{
			hint("无法清理数据表", e);
		}
	}

	protected void doCreateTable()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					getImporter().setTable(getTableName()).doCreate();
					hint("已成功创建数据表" + getTableName());
				}
				catch (Exception e)
				{
					hint("无法创建数据表", e);
				}
			}
		}).start();
	}

	protected void doInsertData()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					getImporter().setTable(getTableName()).doInsert();
					hint("已将数据导入至" + getTableName());
				}
				catch (Exception e)
				{
					hint("无法导入数据", e);
				}
			}
		}).start();
	}

	protected void doLinkDataBase()
	{
		try
		{
			String url = "jdbc:mysql://localhost:3306/" + this.jdbcDbText.getText().trim()
					+ "?useUnicode=true&characterEncoding=UTF-8";

			DataBase db = new GeneralDataBase(url, this.jdbcUsrText.getText().trim(),
					String.valueOf(this.jdbcPwdText.getPassword()));

			SQLKit kit = db.getSQLKit();
			kit.close();
			this.getImporter().setDb(db);
			hint("已成功连接到数据库");
		}
		catch (Exception e)
		{
			hint("无法连接到数据库", e);
		}
	}

	protected void doOpenExcelFile()
	{
		JFileChooser fc = new JFileChooser(this.openDir);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.setFileFilter(new FileFilter()
		{
			@Override
			public boolean accept(File f)
			{
				return f.isDirectory() || f.getName().endsWith(".xlsx");
			}

			@Override
			public String getDescription()
			{
				return null;
			}
		});
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			this.openDir = file.getParentFile();

			String indexText = JOptionPane.showInputDialog(this, "请输入待导入的Sheet序号", "0");
			try
			{
				int index = Integer.parseInt(indexText);
				this.getImporter().setFile(file).setSheetIndex(index).load();
				this.sheetIndexLabel.setText(String.valueOf(this.getImporter().getSheetIndex()));
				this.sheetIndexLabel
						.setToolTipText("第" + String.valueOf(this.getImporter().getSheetIndex()) + "个Sheet");
				this.filePathLabel.setText(this.getImporter().getFile().getAbsolutePath());
				this.filePathLabel.setToolTipText(this.getImporter().getFile().getAbsolutePath());
			}
			catch (NumberFormatException e)
			{
				hint("非法的序号" + indexText, e);
			}
			catch (IOException e)
			{
				hint("无法正确读取Excel", e);
			}
		}
	}

	public ExcelImporter getImporter()
	{
		return importer;
	}

	protected String getTableName()
	{
		return this.tableNameText.getText().trim();
	}

	protected void hint(Exception ex)
	{
		hint("错误", ex);
	}

	protected void hint(String msg)
	{
		JOptionPane.showMessageDialog(this, msg, "提示", JOptionPane.INFORMATION_MESSAGE);
	}

	protected void hint(String title, Exception ex)
	{
		ex.printStackTrace();
		JOptionPane.showMessageDialog(this, makeErrorMsg(ex), title, JOptionPane.ERROR_MESSAGE);
	}

	protected String makeErrorMsg(Exception ex)
	{
		StringBuilder buf = new StringBuilder(ex.getClass().getName() + ": " + ex.getLocalizedMessage());

		int i = 0;
		for (StackTraceElement s : ex.getStackTrace())
		{
			buf.append('\n');
			buf.append(s.toString());
			if (++i >= 12)
			{
				break;
			}
		}

		return buf.toString();
	}

	@Override
	public void onAnalyzed()
	{
		this.progressBar.setValue(0);
	}

	@Override
	public void onAnalyzing(double percent)
	{
		this.progressBar.setValue((int) (100 * percent));
	}

	@Override
	public void onImported()
	{
		this.progressBar.setValue(0);
	}

	@Override
	public void onImporting(double percent)
	{
		this.progressBar.setValue((int) (100 * percent));
	}

	public void setImporter(ExcelImporter importer)
	{
		this.importer = importer;
	}

	public void showApp()
	{
		this.setVisible(true);
	}
}
