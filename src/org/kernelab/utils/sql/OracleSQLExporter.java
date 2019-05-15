package org.kernelab.utils.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.kernelab.basis.Entrance;
import org.kernelab.basis.Tools;
import org.kernelab.basis.io.DataWriter;
import org.kernelab.basis.sql.DataBase;
import org.kernelab.basis.sql.DataBase.Oracle;
import org.kernelab.basis.sql.DataBase.OracleClient;
import org.kernelab.basis.sql.SQLKit;

public class OracleSQLExporter extends DataWriter
{
	public static class OracleColumn
	{
		public static final int						STRING_TYPE_INDEX		= 0;

		public static final int						NUMBER_TYPE_INDEX		= 1;

		public static final int						DATE_TYPE_INDEX			= 2;

		public static final int						TIMESTAMP_TYPE_INDEX	= 3;

		public static final int						CLOB_TYPE_INDEX			= 4;

		public static final int						NCLOB_TYPE_INDEX		= 5;

		public static final int						ROWID_TYPE_INDEX		= 6;

		public static final Map<String, Integer>	TYPE_INDEX_MAP			= new HashMap<String, Integer>();

		static
		{
			TYPE_INDEX_MAP.put("VARCHAR2", STRING_TYPE_INDEX);
			TYPE_INDEX_MAP.put("NVARCHAR2", STRING_TYPE_INDEX);
			TYPE_INDEX_MAP.put("VARCHAR", STRING_TYPE_INDEX);
			TYPE_INDEX_MAP.put("CHAR", STRING_TYPE_INDEX);
			TYPE_INDEX_MAP.put("NCHAR", STRING_TYPE_INDEX);

			TYPE_INDEX_MAP.put("NUMBER", NUMBER_TYPE_INDEX);
			TYPE_INDEX_MAP.put("INTEGER", NUMBER_TYPE_INDEX);
			TYPE_INDEX_MAP.put("INT", NUMBER_TYPE_INDEX);
			TYPE_INDEX_MAP.put("FLOAT", NUMBER_TYPE_INDEX);
			TYPE_INDEX_MAP.put("DECIMAL", NUMBER_TYPE_INDEX);
			TYPE_INDEX_MAP.put("BINARY_FLOAT", NUMBER_TYPE_INDEX);
			TYPE_INDEX_MAP.put("BINARY_DOUBLE", NUMBER_TYPE_INDEX);

			TYPE_INDEX_MAP.put("DATE", DATE_TYPE_INDEX);
			TYPE_INDEX_MAP.put("TIMESTAMP", TIMESTAMP_TYPE_INDEX);

			TYPE_INDEX_MAP.put("CLOB", CLOB_TYPE_INDEX);
			TYPE_INDEX_MAP.put("NCLOB", NCLOB_TYPE_INDEX);

			TYPE_INDEX_MAP.put("ROWID", ROWID_TYPE_INDEX);
			TYPE_INDEX_MAP.put("UROWID", ROWID_TYPE_INDEX);
		}

		public static int appendChar(char c, StringBuilder b, String charSet)
		{
			if (Character.isISOControl(c))
			{
				String asc = String.valueOf((int) c);
				b.append("'||CHR(" + asc + ")||'");
				return 11 + asc.length();
			}
			else if (c == '\'')
			{
				b.append(c);
				b.append(c);
				return 2;
			}
			else
			{
				b.append(c);
				try
				{
					return String.valueOf(c).getBytes(charSet).length;
				}
				catch (UnsupportedEncodingException e)
				{
					return 1;
				}
			}
		}

		public static String exportFormat(int typeIndex, String columnName)
		{
			String format = "\t" + columnName;

			switch (typeIndex)
			{
				case STRING_TYPE_INDEX:
				case NUMBER_TYPE_INDEX:
				case CLOB_TYPE_INDEX:
				case NCLOB_TYPE_INDEX:
					format = columnName + format;
					break;

				case DATE_TYPE_INDEX:
					format = "TO_CHAR(" + columnName + ",'" + DATE_SQL_FORMAT + "')" + format;
					break;

				case TIMESTAMP_TYPE_INDEX:
					format = "TO_CHAR(" + columnName + ",'" + TIMESTAMP_SQL_FORMAT + "')" + format;
					break;

				case ROWID_TYPE_INDEX:
					format = "ROWIDTOCHAR(" + columnName + ")" + format;
					break;
			}

			return format;
		}

		public static Integer getDataTypeIndex(String dataType)
		{
			return TYPE_INDEX_MAP.get(getDataTypeName(dataType));
		}

		public static String getDataTypeName(String dataType)
		{
			return dataType.replaceFirst("^(\\w+)(?:\\(.+?\\))?.*$", "$1").toUpperCase();
		}

		public static String importFormat(int typeIndex, String columnValue)
		{
			return importFormat(typeIndex, columnValue, Charset.defaultCharset().name());
		}

		public static String importFormat(int typeIndex, String columnValue, String charSet)
		{
			if (columnValue == null)
			{
				columnValue = "NULL";
			}
			else
			{
				StringBuilder buffer = new StringBuilder();

				int count = 0;

				switch (typeIndex)
				{
					case STRING_TYPE_INDEX:
						buffer.append('\'');
						count++;
						for (int i = 0; i < columnValue.length(); i++)
						{
							if (LINE_SIZE - count <= 6)
							{
								buffer.append("'||\n'");
								count = 1;
							}

							char c = columnValue.charAt(i);

							count += appendChar(c, buffer, charSet);
						}
						buffer.append('\'');
						break;

					case NUMBER_TYPE_INDEX:
						buffer.append(columnValue);
						break;

					case CLOB_TYPE_INDEX:
						buffer.append("TO_CLOB('");
						count += 9;
						for (int i = 0; i < columnValue.length(); i++)
						{
							if (LINE_SIZE - count <= 6)
							{
								buffer.append("')||\nTO_CLOB('");
								count = 9;
							}

							char c = columnValue.charAt(i);

							count += appendChar(c, buffer, charSet);
						}
						buffer.append("')");
						break;

					case NCLOB_TYPE_INDEX:
						buffer.append("TO_NCLOB('");
						count += 10;
						for (int i = 0; i < columnValue.length(); i++)
						{
							if (LINE_SIZE - count <= 6)
							{
								buffer.append("')||\nTO_NCLOB('");
								count = 10;
							}

							char c = columnValue.charAt(i);

							count += appendChar(c, buffer, charSet);
						}
						buffer.append("')");
						break;

					case DATE_TYPE_INDEX:
						buffer.append("TO_DATE('" + columnValue + "','" + DATE_SQL_FORMAT + "')");
						break;

					case TIMESTAMP_TYPE_INDEX:
						buffer.append("TO_TIMESTAMP('" + columnValue + "','" + TIMESTAMP_SQL_FORMAT + "')");
						break;

					case ROWID_TYPE_INDEX:
						buffer.append("CHARTOROWID('" + columnValue + "')");
						break;
				}

				columnValue = buffer.toString();
			}

			return columnValue;
		}

		private String	columnName;

		private String	dataType;

		private int		dataTypeIndex;

		public OracleColumn(ResultSet rs) throws SQLException
		{
			this(rs.getString("COLUMN_NAME"), rs.getString("DATA_TYPE"));
		}

		public OracleColumn(String columnName, String dataType)
		{
			this.setColumnName(columnName);
			this.setDataType(dataType);
		}

		public String exportFormat()
		{
			return exportFormat(this.getDataTypeIndex(), this.getColumnNameQuote());
		}

		public String getColumnName()
		{
			return columnName;
		}

		public String getColumnNameQuote()
		{
			return "\"" + this.getColumnName() + "\"";
		}

		public String getDataType()
		{
			return dataType;
		}

		public int getDataTypeIndex()
		{
			return dataTypeIndex;
		}

		public String importFormat(ResultSet rs, String charSet) throws SQLException
		{
			return importFormat(this.getDataTypeIndex(), rs.getString(this.getColumnName()), charSet);
		}

		public String importFormat(String value, String charSet)
		{
			return importFormat(this.getDataTypeIndex(), value, charSet);
		}

		public OracleColumn setColumnName(String columnName)
		{
			this.columnName = columnName;
			return this;
		}

		public OracleColumn setDataType(String dataType)
		{
			this.dataType = getDataTypeName(dataType);
			this.setDataTypeIndex(TYPE_INDEX_MAP.get(this.dataType));
			return this;
		}

		private OracleColumn setDataTypeIndex(int dataTypeIndex)
		{
			this.dataTypeIndex = dataTypeIndex;
			return this;
		}
	}

	public static final String	INSERT_MODE				= "insert";

	public static final String	UPDATE_MODE				= "update";

	public static final String	MERGE_MODE				= "merge";

	public static String		DATE_JAVA_FORMAT		= "yyyy-MM-dd HH:mm:ss";

	public static String		TIMESTAMP_JAVA_FORMAT	= "yyyy-MM-dd HH:mm:ss.SSS";

	public static String		DATE_SQL_FORMAT			= "YYYY-MM-DD HH24:MI:SS";

	public static String		TIMESTAMP_SQL_FORMAT	= "YYYY-MM-DD HH24:MI:SS.FF";

	public static String		TABLE_COLUMNS			= "ALL_TAB_COLUMNS";

	public static int			LINE_SIZE				= 500;

	private static void debug() throws FileNotFoundException, SQLException
	{
		OracleSQLExporter e = new OracleSQLExporter();

		DataBase dataBase = new Oracle("localhost", 1521, "orcl", "test", "test");
		e.setKit(dataBase.getSQLKit());

		// e.setDataFile(new File("./dat/demo_table.sql"), false, "UTF-8");

		Writer buffer = new StringWriter();
		e.setWriter(buffer);

		e.resetTableRule("demo_table", "ORDER BY id").resetTableExportColumns().resetTablePrimaryKeys("id")
				.exportMerges().close();

		Tools.debug(buffer.toString());

		Tools.debug("DONE.");
	}

	public static String Insert(String table, Map<String, Object> row)
	{
		if (table == null || row == null || row.isEmpty())
		{
			return null;
		}

		StringBuilder buffer = new StringBuilder();

		buffer.append("INSERT INTO ");
		buffer.append(table);

		boolean first = true;

		List<String> items = new LinkedList<String>();

		for (Entry<String, Object> entry : row.entrySet())
		{
			if (!first)
			{
				buffer.append(",\n");
			}
			else
			{
				first = false;
				buffer.append(" (");
			}
			buffer.append(entry.getKey());

			items.add(SQLValue(entry.getValue()));
		}

		buffer.append(")\n VALUES (");
		Tools.jointStrings(buffer, ",\n", items);
		buffer.append(");");

		return buffer.toString();
	}

	/**
	 * @param args
	 * @throws FileNotFoundException
	 * @throws SQLException
	 * @throws UnsupportedEncodingException
	 */
	public static void main(String[] args) throws FileNotFoundException, SQLException
	{
		boolean product = true;

		if (product)
		{
			Entrance ent = new Entrance().handle(args);

			String table = ent.parameter("table");
			List<String> columns = ent.parameters("columns", true);
			List<String> keys = ent.parameters("keys", true);
			String rule = ent.parameter("rule", "");

			String file = ent.parameter("file");
			String charset = ent.parameter("charset", Charset.defaultCharset().name());

			String mode = ent.parameter("mode");

			String user = ent.parameter("user");
			String pass = ent.parameter("pass");
			String tns = ent.parameter("tns");

			if (table != null && tns != null && user != null && pass != null)
			{
				if (mode == null)
				{
					if (keys.isEmpty())
					{
						mode = INSERT_MODE;
					}
					else
					{
						mode = MERGE_MODE;
					}
				}
				else
				{
					mode = mode.trim().toLowerCase();
				}

				if (INSERT_MODE.equals(mode) || UPDATE_MODE.equals(mode) || MERGE_MODE.equals(mode))
				{
					if (file == null)
					{
						file = "./" + table + "_" + mode + ".sql";
					}

					OracleSQLExporter e = new OracleSQLExporter();

					e.setCharsetName(charset);

					e.setDataFile(new File(file), false, charset);

					DataBase dataBase = new OracleClient(tns, user, pass);

					e.setKit(dataBase.getSQLKit()).resetTableRule(table, rule).resetTableExportColumns(columns)
							.resetTablePrimaryKeys(keys);

					if (INSERT_MODE.equals(mode))
					{
						e.exportInserts();
					}
					else if (UPDATE_MODE.equals(mode))
					{
						e.exportUpdates();
					}
					else
					{
						e.exportMerges();
					}

					e.close();
				}
			}
			else
			{
				ent.present();
			}
		}
		else
		{
			debug();
		}
	}

	public static String SQLValue(Date value)
	{
		return OracleColumn.importFormat(OracleColumn.DATE_TYPE_INDEX,
				Tools.getDateTimeString(value, DATE_JAVA_FORMAT));
	}

	public static String SQLValue(Number value)
	{
		return OracleColumn.importFormat(OracleColumn.NUMBER_TYPE_INDEX, String.valueOf(value));
	}

	public static String SQLValue(Object value)
	{
		String string = null;

		if (value == null)
		{
			string = "NULL";
		}
		else if (value instanceof String)
		{
			string = SQLValue((String) value);
		}
		else if (value instanceof Number)
		{
			string = SQLValue((Number) value);
		}
		else if (value instanceof Character)
		{
			string = SQLValue(((Character) value).toString());
		}
		else if (value instanceof Date)
		{
			string = SQLValue((Date) value);
		}
		else if (value instanceof Calendar)
		{
			string = SQLValue(new Date(((Calendar) value).getTimeInMillis()));
		}
		else if (value instanceof Timestamp)
		{
			string = SQLValue((Timestamp) value);
		}
		else
		{
			string = SQLValue(value.toString());
		}

		return string;
	}

	public static String SQLValue(String value)
	{
		return OracleColumn.importFormat(OracleColumn.STRING_TYPE_INDEX, value);
	}

	public static String SQLValue(Timestamp value)
	{
		return OracleColumn.importFormat(OracleColumn.TIMESTAMP_TYPE_INDEX,
				Tools.getDateTimeString(value, TIMESTAMP_JAVA_FORMAT));
	}

	private SQLKit						kit;

	private String						table;

	private String						rule;

	private Map<String, OracleColumn>	columns		= new LinkedHashMap<String, OracleColumn>();

	private Set<String>					exports		= new LinkedHashSet<String>();

	private Set<String>					primaryKeys	= new LinkedHashSet<String>();

	private int							commitRows;

	public OracleSQLExporter exportInserts() throws SQLException
	{
		if (this.table != null)
		{
			this.fetchTableColumns();

			String insert = "INSERT INTO " + this.table + " (\n";
			String split = "";
			for (OracleColumn c : columns.values())
			{
				if (this.exports.contains(c.getColumnName()))
				{
					insert += split;
					insert += c.getColumnNameQuote();
					split = ",\n";
				}
			}
			insert += ")\n";

			int count = 0;
			ResultSet rs = this.fetchTableData();
			while (rs.next())
			{
				this.print(insert);
				this.print("VALUES (");
				split = "";
				for (OracleColumn c : columns.values())
				{
					if (this.exports.contains(c.getColumnName()))
					{
						this.print(split + "\n");
						this.print(c.importFormat(rs, this.getCharsetName()));
						split = ",";
					}
				}
				this.print("\n);\n");

				count++;
				if (count >= this.getCommitRows() && this.getCommitRows() > 0)
				{
					count = 0;
					this.printCommit();
				}
			}

			this.table = null;
		}

		return this;
	}

	public OracleSQLExporter exportMerges() throws SQLException
	{
		if (this.table != null && !this.primaryKeys.isEmpty())
		{
			this.fetchTableColumns();

			this.print("MERGE INTO " + this.table + "\tT USING (\n");

			ResultSet rs = this.fetchTableData();

			String split = "";
			while (rs.next())
			{
				this.print(split);

				this.print("SELECT \n");

				String columnSplit = "";

				for (OracleColumn c : columns.values())
				{
					if (this.exports.contains(c.getColumnName()) || this.primaryKeys.contains(c.getColumnName()))
					{
						this.print(columnSplit);
						this.print(c.importFormat(rs, this.getCharsetName()));
						if (split.length() == 0)
						{
							this.print("\t");
							this.print(c.getColumnNameQuote());
						}
						columnSplit = ",\n";
					}
				}

				this.print(" \nFROM DUAL");

				split = "\n UNION ALL \n";
			}

			this.print(")\tS \nON (");

			split = "";
			for (String key : this.primaryKeys)
			{
				this.print(split);
				this.print("T." + key + "=S." + key);
				split = "\n AND ";
			}
			this.print(")");

			this.print(" \nWHEN MATCHED THEN UPDATE SET ");
			split = "";
			for (OracleColumn c : columns.values())
			{
				if (this.exports.contains(c.getColumnName()) && !this.primaryKeys.contains(c.getColumnName()))
				{
					this.print(split + "\n");
					this.print("T." + c.getColumnNameQuote() + "=S." + c.getColumnNameQuote());
					split = ",";
				}
			}

			this.print(" \nWHEN NOT MATCHED THEN INSERT (");
			split = "";
			for (OracleColumn c : columns.values())
			{
				if (this.exports.contains(c.getColumnName()))
				{
					this.print(split + "\n");
					this.print("T." + c.getColumnNameQuote());
					split = ",";
				}
			}

			this.print(") VALUES (");
			split = "";
			for (OracleColumn c : columns.values())
			{
				if (this.exports.contains(c.getColumnName()))
				{
					this.print(split + "\n");
					this.print("S." + c.getColumnNameQuote());
					split = ",";
				}
			}

			this.print(");");
		}
		return this;
	}

	public OracleSQLExporter exportUpdates() throws SQLException
	{
		if (this.table != null && !this.primaryKeys.isEmpty())
		{
			this.fetchTableColumns();

			String update = "UPDATE " + this.table;
			String split = "";

			int count = 0;
			ResultSet rs = this.fetchTableData();
			while (rs.next())
			{
				this.print(update);
				this.print(" SET ");

				split = "";
				for (OracleColumn c : columns.values())
				{
					if (this.exports.contains(c.getColumnName()) && !this.primaryKeys.contains(c.getColumnName()))
					{
						this.print(split + "\n");
						this.print(c.getColumnNameQuote() + "=");
						this.print(c.importFormat(rs, this.getCharsetName()));
						split = ",";
					}
				}
				this.print(" \nWHERE ");

				split = "";
				for (String key : this.primaryKeys)
				{
					this.print(split + "\n");
					this.print(key + "=");
					this.print(this.columns.get(key).importFormat(rs, this.getCharsetName()));
					split = " AND ";
				}

				this.print(";\n");

				count++;
				if (count >= this.getCommitRows() && this.getCommitRows() > 0)
				{
					count = 0;
					this.printCommit();
				}
			}
		}
		return this;
	}

	protected OracleSQLExporter fetchTableColumns() throws SQLException
	{
		String columnsCondition = "";

		if (!this.exports.isEmpty())
		{
			Set<String> usingColumns = new HashSet<String>();

			usingColumns.addAll(this.exports);
			usingColumns.addAll(this.primaryKeys);

			columnsCondition = " AND COLUMN_NAME IN ('" + Tools.jointStrings("','", usingColumns) + "')";
		}

		String sql = "SELECT * FROM " + TABLE_COLUMNS + " WHERE TABLE_NAME=?" + columnsCondition
				+ " ORDER BY COLUMN_ID ASC";
		ResultSet rs = kit.query(sql, this.table);
		this.columns.clear();
		while (rs.next())
		{
			OracleColumn column = new OracleColumn(rs);
			this.columns.put(column.getColumnName(), column);
		}

		if (this.exports.isEmpty())
		{
			for (OracleColumn c : this.columns.values())
			{
				this.exports.add(c.getColumnName());
			}
		}

		return this;
	}

	protected ResultSet fetchTableData() throws SQLException
	{
		String sql = "";
		String split = "";
		for (OracleColumn c : this.columns.values())
		{
			sql += split;
			sql += c.exportFormat();
			split = ",";
		}

		sql = "SELECT " + sql + " FROM " + this.table + " " + this.rule;

		return this.kit.query(sql);
	}

	public int getCommitRows()
	{
		return commitRows;
	}

	public SQLKit getKit()
	{
		return kit;
	}

	protected void printCommit()
	{
		this.print("COMMIT;\n");
	}

	public OracleSQLExporter resetTableExportColumns(Iterable<String> columns)
	{
		this.exports.clear();
		for (String column : columns)
		{
			this.exports.add(column.trim().toUpperCase());
		}
		return this;
	}

	public OracleSQLExporter resetTableExportColumns(String... columns)
	{
		this.exports.clear();
		for (String column : columns)
		{
			this.exports.add(column.trim().toUpperCase());
		}
		return this;
	}

	public OracleSQLExporter resetTablePrimaryKeys(Iterable<String> primaryKeys)
	{
		this.primaryKeys.clear();
		for (String key : primaryKeys)
		{
			this.primaryKeys.add(key.trim().toUpperCase());
		}
		return this;
	}

	public OracleSQLExporter resetTablePrimaryKeys(String... primaryKeys)
	{
		this.primaryKeys.clear();
		for (String key : primaryKeys)
		{
			this.primaryKeys.add(key.trim().toUpperCase());
		}
		return this;
	}

	public OracleSQLExporter resetTableRule(String table, String rule)
	{
		this.table = table.toUpperCase();
		this.rule = rule;
		return this;
	}

	public OracleSQLExporter setCommitRows(int commitRows)
	{
		this.commitRows = commitRows;
		return this;
	}

	public OracleSQLExporter setKit(SQLKit kit)
	{
		this.kit = kit;
		return this;
	}
}
