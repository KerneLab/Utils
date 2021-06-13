package org.kernelab.utils.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kernelab.basis.Canal;
import org.kernelab.basis.Canal.Action;
import org.kernelab.basis.Canal.Tuple2;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.DataBase;
import org.kernelab.basis.sql.DataBase.GeneralDataBase;
import org.kernelab.basis.sql.SQLKit;

public class ExcelImporter
{
	public static class Column
	{
		public static final String	TYPE_STRING		= "VARCHAR";

		public static final String	TYPE_NUMBER		= "NUMBER";

		public static final String	TYPE_DATETIME	= "DATETIME";

		private String				name			= null;

		private String				type			= null;

		private int					length			= 0;

		private int					scale			= 0;

		public Column(String name)
		{
			this.setName(name);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Column other = (Column) obj;
			if (name == null)
			{
				if (other.name != null)
					return false;
			}
			else if (!name.equals(other.name))
				return false;
			return true;
		}

		public int getLength()
		{
			return length;
		}

		public String getName()
		{
			return name;
		}

		public int getScale()
		{
			return scale;
		}

		public String getType()
		{
			return type;
		}

		protected String getTypeExpr()
		{
			if (this.getType() == TYPE_STRING)
			{
				return TYPE_STRING + "(" + this.getLength() + ")";
			}
			else if (this.getType() == TYPE_NUMBER)
			{
				if (this.getScale() == 0 && this.getLength() <= 10)
				{
					return "INT(" + this.getLength() + ")";
				}
				else
				{
					return "DECIMAL(" + this.getLength() + "," + this.getScale() + ")";
				}
			}
			else if (this.getType() == TYPE_DATETIME)
			{
				return TYPE_DATETIME;
			}
			else
			{
				return null;
			}
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		public void sample(Object value)
		{
			if (value == null)
			{
				return;
			}

			if (this.getType() == null)
			{
				if (value instanceof String)
				{
					this.setType(TYPE_STRING);
				}
				else if (value instanceof Number)
				{
					this.setType(TYPE_NUMBER);
				}
				else if (value instanceof Date)
				{
					this.setType(TYPE_DATETIME);
				}
			}

			if (this.getType() == TYPE_STRING)
			{
				this.setLength(Math.max(value.toString().length(), this.getLength()));
			}
			else if (this.getType() == TYPE_NUMBER)
			{
				BigDecimal num = (BigDecimal) value;
				this.setLength(Math.max(num.precision(), this.getLength()));
				this.setScale(Math.max(num.scale(), this.getScale()));
			}
		}

		public void setLength(int length)
		{
			this.length = length;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public void setScale(int scale)
		{
			this.scale = scale;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		@Override
		public String toString()
		{
			return "`" + this.getName() + "` " + this.getTypeExpr() + " NULL";
		}
	}

	public static class DefaultImportListener implements ImportListener
	{
		protected final int				width;

		protected final int				total;

		protected final StringBuilder	buf;

		public DefaultImportListener(int total)
		{
			this.total = total;

			this.width = total - 2 - 6;

			this.buf = new StringBuilder(this.total * 2);

			for (int i = 0; i < this.total; i++)
			{
				this.buf.append('\b');
			}
		}

		@Override
		public void onAnalyzed()
		{
			System.out.println();
			System.out.flush();
		}

		@Override
		public void onAnalyzing(double percent)
		{
			refresh(percent);
		}

		@Override
		public void onImported()
		{
			System.out.println();
			System.out.flush();
		}

		@Override
		public void onImporting(double percent)
		{
			refresh(percent);
		}

		protected void refresh(double percent)
		{
			buf.delete(total, buf.length());

			buf.append('[');

			int len = (int) (percent * width);

			for (int i = 0; i < len; i++)
			{
				buf.append('>');
			}

			for (int i = 0; i < width - len; i++)
			{
				buf.append(' ');
			}

			buf.append(']');

			buf.append(String.format("%5.2f%%", percent * 100));

			System.out.print(buf.toString());
			System.out.flush();
		}
	}

	public static interface ImportListener
	{
		public void onAnalyzed();

		public void onAnalyzing(double percent);

		public void onImported();

		public void onImporting(double percent);
	}

	public static Object CellContent(Cell cell, FormulaEvaluator fe)
	{
		Object content = null;

		CellValue value = fe.evaluate(cell);

		if (value != null)
		{
			switch (value.getCellType())
			{
				case Cell.CELL_TYPE_STRING:
					content = value.getStringValue();
					break;

				case Cell.CELL_TYPE_NUMERIC:
					if (DateUtil.isCellDateFormatted(cell))
					{
						content = cell.getDateCellValue();
					}
					else
					{
						content = new BigDecimal(value.formatAsString()).stripTrailingZeros();
					}
					break;

				case Cell.CELL_TYPE_BOOLEAN:
					content = value.getBooleanValue() ? "Y" : "N";
					break;

				case Cell.CELL_TYPE_BLANK:
					break;

				case Cell.CELL_TYPE_ERROR:
					break;
			}
		}

		return content;
	}

	public static void main(String[] args)
	{
		File file = new File("./dat/test.xlsx");

		try
		{
			String url = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8";

			ExcelImporter imp = new ExcelImporter() //
					.addListener(new DefaultImportListener(50)) //
					.setDb(new GeneralDataBase(url, "test", "test")) //
					.setFile(file) //
					.setSheetIndex(0) //
					.setTable("jdl_test_import") //
			;

			Tools.debug("Analyzing...");
			Tools.debug(imp.makeCreateTable());

			Tools.debug("Cleaning...");
			imp.doClean();

			Tools.debug("Importing...");
			imp.doInsert();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private File						file;

	private InputStream					inputStream;

	private String[]					sheets;

	private int							sheetIndex;

	private String						table;

	private Workbook					workbook;

	private FormulaEvaluator			fe;

	private Sheet						sheet;

	private Column[]					meta;

	private DataBase					db;

	private int							batchs		= 1000;

	private int							dataRows;

	private Collection<ImportListener>	listeners	= new LinkedHashSet<ImportListener>();

	public ExcelImporter()
	{
	}

	public ExcelImporter addListener(ImportListener listener)
	{
		if (listener != null)
		{
			this.getListeners().add(listener);
		}
		return this;
	}

	protected Column[] analyzeMetaByBody()
	{
		final Column[] meta = this.getMeta();

		Canal.of(this.getSheet()).skip(this.getSheet().getFirstRowNum() + 1).foreach(new Action<Row>()
		{
			int count = 0;

			@Override
			public void action(Row r)
			{
				for (int c = 0; c < meta.length; c++)
				{
					meta[c].sample(CellContent(r.getCell(c), getFe()));
				}

				count++;

				fireAnalyzing(count);
			}
		});

		this.fireAnalyzed();

		return meta;
	}

	public ExcelImporter doClean() throws SQLException
	{
		SQLKit kit = this.getDb().getSQLKit();
		try
		{
			String sql = this.makeClean();

			Tools.debug(sql);

			kit.execute(sql);

			return this;
		}
		finally
		{
			if (kit != null)
			{
				kit.close();
			}
		}
	}

	public ExcelImporter doCreate() throws SQLException
	{
		SQLKit kit = this.getDb().getSQLKit();

		try
		{
			String sql = this.makeCreateTable();

			Tools.debug(sql);

			kit.execute(sql);

			return this;
		}
		finally
		{
			if (kit != null)
			{
				kit.close();
			}
		}
	}

	public ExcelImporter doDrop() throws SQLException
	{
		SQLKit kit = this.getDb().getSQLKit();

		try
		{
			String sql = this.makeDropTable();

			Tools.debug(sql);

			kit.execute(sql);

			return this;
		}
		finally
		{
			if (kit != null)
			{
				kit.close();
			}
		}
	}

	public ExcelImporter doInsert() throws SQLException
	{
		final SQLKit kit = this.getDb().getSQLKit();

		try
		{
			String sql = this.makeInsert();
			Tools.debug(sql);
			kit.setAutoCommit(false);
			kit.prepareStatement(sql);
			Canal.of(this.getSheet()).skip(this.getSheet().getFirstRowNum() + 1).map(new Mapper<Row, Object[]>()
			{
				@Override
				public Object[] map(Row row)
				{
					return Canal.of(row).zipWithIndex().map(new Mapper<Tuple2<Cell, Long>, Object>()
					{
						@Override
						public Object map(Tuple2<Cell, Long> el)
						{
							return CellContent(el._1, getFe());
						}
					}).collect().toArray();
				}
			}).foreach(new Action<Object[]>()
			{
				int count = 0;

				@Override
				public void action(Object[] el)
				{
					try
					{
						kit.addBatch(el);

						count++;

						if (count % getBatchs() == 0)
						{
							kit.commitBatch();
						}

						fireImporting(count);
					}
					catch (SQLException e)
					{
						throw new RuntimeException(e);
					}
				}
			});

			kit.commitBatch();

			this.fireImported();

			return this;
		}
		finally
		{
			if (kit != null)
			{
				kit.close();
			}
		}
	}

	protected void fireAnalyzed()
	{
		for (ImportListener l : this.getListeners())
		{
			l.onAnalyzed();
		}
	}

	protected void fireAnalyzing(int count)
	{
		for (ImportListener l : this.getListeners())
		{
			l.onAnalyzing(progress(count));
		}
	}

	protected void fireImported()
	{
		for (ImportListener l : this.getListeners())
		{
			l.onImported();
		}
	}

	protected void fireImporting(int count)
	{
		for (ImportListener l : this.getListeners())
		{
			l.onImporting(progress(count));
		}
	}

	public int getBatchs()
	{
		return batchs;
	}

	protected int getDataRows()
	{
		return dataRows;
	}

	public DataBase getDb()
	{
		return db;
	}

	protected FormulaEvaluator getFe()
	{
		return fe;
	}

	public File getFile()
	{
		return file;
	}

	public InputStream getInputStream()
	{
		return inputStream;
	}

	protected Collection<ImportListener> getListeners()
	{
		return listeners;
	}

	protected Column[] getMeta()
	{
		return meta;
	}

	protected Sheet getSheet()
	{
		return sheet;
	}

	public int getSheetIndex()
	{
		return sheetIndex;
	}

	public String[] getSheets()
	{
		return sheets;
	}

	public String getTable()
	{
		return table;
	}

	protected Workbook getWorkbook()
	{
		return workbook;
	}

	protected String makeClean()
	{
		return "TRUNCATE TABLE `" + this.getTable() + "`";
	}

	protected String makeCreateTable()
	{
		this.analyzeMetaByBody();

		return "CREATE TABLE `" + this.getTable() + "` (\r\n"
				+ Canal.of(new String[] { "`ROWID` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT" }) //
						.union(Canal.of(this.getMeta()).map(new Mapper<Column, String>()
						{
							@Override
							public String map(Column col)
							{
								return col.toString();
							}
						})) //
						.union(Canal.of(new String[] { "PRIMARY KEY (`ROWID`)" })) //
						.toString(",\r\n") //
				+ "\r\n) COLLATE='utf8mb4_bin' ENGINE=InnoDB";
	}

	protected String makeDropTable()
	{
		return "DROP TABLE `" + this.getTable() + "`";
	}

	protected String makeInsert()
	{
		return "INSERT INTO `" + this.getTable() + "` " + Canal.of(this.getMeta()).map(new Mapper<Column, String>()
		{
			@Override
			public String map(Column el)
			{
				return "`" + el.getName() + "`";
			}
		}).toString(",", "(", ")") //
				+ " VALUES " + Canal.of(this.getMeta()).map(new Mapper<Column, String>()
				{
					@Override
					public String map(Column el)
					{
						return "?";
					}
				}).toString(",", "(", ")");
	}

	protected Column[] makeMetaByHeader()
	{
		Row head = this.getSheet().getRow(this.getSheet().getFirstRowNum());

		Column[] meta = Canal.of(head).map(new Mapper<Cell, Column>()
		{
			@Override
			public Column map(Cell cell)
			{
				return new Column(CellContent(cell, getFe()).toString());
			}
		}).collect().toArray(new Column[0]);

		return meta;
	}

	protected double progress(int process)
	{
		return Tools.limitNumber(1.0 * process / this.getDataRows(), 0.0, 1.0);
	}

	public ExcelImporter setBatchs(int batchs)
	{
		this.batchs = batchs;
		return this;
	}

	protected void setDataRows(int dataRows)
	{
		this.dataRows = dataRows;
	}

	public ExcelImporter setDb(DataBase db)
	{
		this.db = db;
		return this;
	}

	protected ExcelImporter setFe(FormulaEvaluator fe)
	{
		this.fe = fe;
		return this;
	}

	public ExcelImporter setFile(File file) throws FileNotFoundException, IOException
	{
		this.file = file;

		if (file != null)
		{
			this.setInputStream(new FileInputStream(file));
		}

		return this;
	}

	public ExcelImporter setInputStream(InputStream is) throws IOException
	{
		this.inputStream = is;

		if (is != null)
		{
			this.setWorkbook(new XSSFWorkbook(is));
		}

		return this;
	}

	protected ExcelImporter setListeners(Collection<ImportListener> listeners)
	{
		this.listeners = listeners;
		return this;
	}

	protected ExcelImporter setMeta(Column[] meta)
	{
		this.meta = meta;
		return this;
	}

	protected ExcelImporter setSheet(Sheet sheet)
	{
		this.sheet = sheet;
		return this;
	}

	public ExcelImporter setSheetIndex(int index)
	{
		this.sheetIndex = index;

		if (index >= 0)
		{
			this.setSheet(this.getWorkbook().getSheetAt(index));
			this.setDataRows(this.getSheet().getLastRowNum() - this.getSheet().getFirstRowNum());
			this.setMeta(this.makeMetaByHeader());
		}

		return this;
	}

	public ExcelImporter setSheets(String[] sheets)
	{
		this.sheets = sheets;
		return this;
	}

	public ExcelImporter setTable(String table)
	{
		this.table = table;
		return this;
	}

	protected ExcelImporter setWorkbook(Workbook wb)
	{
		this.workbook = wb;

		if (wb != null)
		{
			this.setFe(wb.getCreationHelper().createFormulaEvaluator());

			int sheets = wb.getNumberOfSheets();

			List<String> names = new LinkedList<String>();
			for (int i = 0; i < sheets; i++)
			{
				names.add(wb.getSheetName(i));
			}

			this.setSheets(names.toArray(new String[0]));
		}

		return this;
	}
}
