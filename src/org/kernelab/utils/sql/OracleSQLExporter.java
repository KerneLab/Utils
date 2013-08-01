package org.kernelab.utils.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kernelab.basis.DataWriter;
import org.kernelab.basis.Entrance;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.DataBase;
import org.kernelab.basis.sql.DataBase.Oracle;
import org.kernelab.basis.sql.DataBase.OracleClassic;
import org.kernelab.basis.sql.SQLKit;

public class OracleSQLExporter extends DataWriter
{
	protected static class OracleColumn
	{
		public static final int						STRING_TYPE_INDEX		= 0;

		public static final int						NUMBER_TYPE_INDEX		= 1;

		public static final int						DATE_TYPE_INDEX			= 2;

		public static final int						TIMESTAMP_TYPE_INDEX	= 3;

		public static final int						CLOB_TYPE_INDEX			= 4;

		public static final int						NCLOB_TYPE_INDEX		= 5;

		public static final Map<String, Integer>	TYPE_INDEX_MAP			= new HashMap<String, Integer>();

		static
		{
			TYPE_INDEX_MAP.put("VARCHAR2", STRING_TYPE_INDEX);
			TYPE_INDEX_MAP.put("NVARCHAR2", STRING_TYPE_INDEX);
			TYPE_INDEX_MAP.put("CHAR", STRING_TYPE_INDEX);
			TYPE_INDEX_MAP.put("NUMBER", NUMBER_TYPE_INDEX);
			TYPE_INDEX_MAP.put("DATE", DATE_TYPE_INDEX);
			TYPE_INDEX_MAP.put("CLOB", CLOB_TYPE_INDEX);
			TYPE_INDEX_MAP.put("NCLOB", NCLOB_TYPE_INDEX);
		}

		private String								columnName;

		private String								dataType;

		private int									dataTypeIndex;

		public OracleColumn(ResultSet rs) throws SQLException
		{
			this.columnName = rs.getString("COLUMN_NAME");
			this.dataType = rs.getString("DATA_TYPE");
			this.dataTypeIndex = TYPE_INDEX_MAP.get(this.dataType);
		}

		public String exportFormat()
		{
			String format = "\t" + columnName;

			switch (this.dataTypeIndex)
			{
				case STRING_TYPE_INDEX:
				case NUMBER_TYPE_INDEX:
				case CLOB_TYPE_INDEX:
				case NCLOB_TYPE_INDEX:
					format = columnName + format;
					break;

				case DATE_TYPE_INDEX:
					format = "TO_CHAR(" + columnName + ",'YYYY-MM-DD HH24:MI:SS')" + format;
					break;

				case TIMESTAMP_TYPE_INDEX:
					format = "TO_CHAR(" + columnName + ",'YYYY-MM-DD HH24:MI:SS.FF')" + format;
					break;
			}

			return format;
		}

		public String importFormat(ResultSet rs) throws SQLException
		{
			String format = rs.getString(this.columnName);

			if (format == null)
			{
				format = "NULL";
			}
			else
			{
				StringBuilder buffer = new StringBuilder();

				int count = 0;

				switch (this.dataTypeIndex)
				{
					case STRING_TYPE_INDEX:
						buffer.append('\'');
						count++;
						for (int i = 0; i < format.length(); i++)
						{
							if (LINE_SIZE - count <= 6)
							{
								buffer.append("'||\n'");
								count = 1;
							}
							char c = format.charAt(i);

							switch (c)
							{
								case '\'':
									buffer.append(c);
									buffer.append(c);
									count += 2;
									break;

								case '\n':
									buffer.append("'||CHR(10)||'");
									count += 13;
									break;

								default:
									buffer.append(c);
									count++;
									break;
							}

						}
						buffer.append('\'');
						break;

					case NUMBER_TYPE_INDEX:
						buffer.append(format);
						break;

					case CLOB_TYPE_INDEX:
						buffer.append("TO_CLOB('");
						count += 9;
						for (int i = 0; i < format.length(); i++)
						{
							if (LINE_SIZE - count <= 6)
							{
								buffer.append("')||\nTO_CLOB('");
								count = 9;
							}

							char c = format.charAt(i);

							switch (c)
							{
								case '\'':
									buffer.append(c);
									buffer.append(c);
									count += 2;
									break;

								case '\n':
									buffer.append("'||CHR(10)||'");
									count += 13;
									break;

								default:
									buffer.append(c);
									count++;
									break;
							}
						}
						buffer.append("')");
						break;

					case NCLOB_TYPE_INDEX:
						buffer.append("TO_NCLOB('");
						count += 10;
						for (int i = 0; i < format.length(); i++)
						{
							if (LINE_SIZE - count <= 6)
							{
								buffer.append("')||\nTO_NCLOB('");
								count = 10;
							}

							char c = format.charAt(i);

							switch (c)
							{
								case '\'':
									buffer.append(c);
									buffer.append(c);
									count += 2;
									break;

								case '\n':
									buffer.append("'||CHR(10)||'");
									count += 13;
									break;

								default:
									buffer.append(c);
									count++;
									break;
							}
						}
						buffer.append("')");
						break;

					case DATE_TYPE_INDEX:
						buffer.append("TO_DATE('" + format + "','YYYY-MM-DD HH24:MI:SS')");
						break;

					case TIMESTAMP_TYPE_INDEX:
						buffer.append("TO_TIMESTAMP('" + format + "','YYYY-MM-DD HH24:MI:SS.FF')");
						break;
				}

				format = buffer.toString();
			}

			return format;
		}
	}

	public static final String	INSERT_MODE	= "insert";

	public static final String	UPDATE_MODE	= "update";

	public static int			LINE_SIZE	= 500;

	private static void debug() throws FileNotFoundException, SQLException
	{
		OracleSQLExporter e = new OracleSQLExporter();

		DataBase dataBase = new Oracle("localhost", 1521, "orcl", "test", "test");
		e.setKit(dataBase.getSQLKit());

		e.setDataFile(new File("./dat/demo_table.sql"), false, "UTF-8");
		e.resetTableRule("demo_table", "ORDER BY id").resetTableExportColumns().exportInserts().close();

		Tools.debug("DONE.");
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
			Entrance ent = new Entrance().gather(args);

			String table = ent.parameter("table");
			List<String> columns = ent.parameters("columns", true);
			List<String> keys = ent.parameters("keys", true);
			String rule = ent.parameter("rule", "");

			String file = ent.parameter("file");
			String charset = ent.parameter("charset", Charset.defaultCharset().name());

			String mode = ent.parameter("mode");

			String ip = ent.parameter("ip", "localhost");
			int port = Integer.parseInt(ent.parameter("port", "1521"));
			String db = ent.parameter("db");
			String user = ent.parameter("user");
			String pass = ent.parameter("pass");

			if (table != null && db != null && user != null && pass != null)
			{
				if (mode == null)
				{
					if (keys.isEmpty())
					{
						mode = INSERT_MODE;
					}
					else
					{
						mode = UPDATE_MODE;
					}
				}
				else
				{
					mode = mode.trim().toLowerCase();
				}

				if (INSERT_MODE.equals(mode) || UPDATE_MODE.equals(mode))
				{
					if (file == null)
					{
						file = "./" + table + "_" + mode + ".sql";
					}

					OracleSQLExporter e = new OracleSQLExporter();

					e.setDataFile(new File(file), false, charset);

					DataBase dataBase = new OracleClassic(ip, port, db, user, pass);

					e.setKit(dataBase.getSQLKit()).resetTableRule(table, rule).resetTableExportColumns(columns)
							.resetTablePrimaryKeys(keys);

					if (INSERT_MODE.equals(mode))
					{
						e.exportInserts();
					}
					else
					{
						e.exportUpdates();
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

	private SQLKit						kit;

	private String						table;

	private String						rule;

	private Map<String, OracleColumn>	columns		= new LinkedHashMap<String, OracleColumn>();

	private Set<String>					exports		= new LinkedHashSet<String>();

	private Set<String>					primaryKeys	= new LinkedHashSet<String>();

	public OracleSQLExporter exportInserts() throws SQLException
	{
		if (this.table != null)
		{
			this.fetchTableColumns();

			String insert = "INSERT INTO " + this.table + " (\n";
			String split = "";
			for (OracleColumn c : columns.values())
			{
				if (this.exports.contains(c.columnName))
				{
					insert += split;
					insert += c.columnName;
					split = ",\n";
				}
			}
			insert += ")\n";

			ResultSet rs = this.fetchTableData();
			while (rs.next())
			{
				this.print(insert);
				this.print("VALUES (");
				split = "";
				for (OracleColumn c : columns.values())
				{
					if (this.exports.contains(c.columnName))
					{
						this.print(split + "\n");
						this.print(c.importFormat(rs));
						split = ",";
					}
				}
				this.print("\n);\n");
			}

			this.table = null;
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

			ResultSet rs = this.fetchTableData();
			while (rs.next())
			{
				this.print(update);
				this.print(" SET ");

				split = "";
				for (OracleColumn c : columns.values())
				{
					if (this.exports.contains(c.columnName) && !this.primaryKeys.contains(c.columnName))
					{
						this.print(split + "\n");
						this.print(c.columnName + "=");
						this.print(c.importFormat(rs));
						split = ",";
					}
				}
				this.print(" \nWHERE ");

				split = "";
				for (String key : this.primaryKeys)
				{
					this.print(split + "\n");
					this.print(key + "=");
					this.print(this.columns.get(key).importFormat(rs));
					split = " AND ";
				}

				this.print(";\n");
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

		String sql = "SELECT * FROM USER_TAB_COLUMNS WHERE TABLE_NAME=?" + columnsCondition + " ORDER BY COLUMN_ID ASC";
		ResultSet rs = kit.query(sql, this.table);
		this.columns.clear();
		while (rs.next())
		{
			OracleColumn column = new OracleColumn(rs);
			this.columns.put(column.columnName, column);
		}

		if (this.exports.isEmpty())
		{
			for (OracleColumn c : this.columns.values())
			{
				this.exports.add(c.columnName);
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

	public SQLKit getKit()
	{
		return kit;
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

	public OracleSQLExporter setKit(SQLKit kit)
	{
		this.kit = kit;
		return this;
	}
}