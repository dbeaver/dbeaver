package org.jkiss.dbeaver.ext.dm.model;

import java.util.Arrays;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.dm.data.DmBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.utils.ArrayUtils;

public class DmSQLDialect extends JDBCSQLDialect {

	public static final String[] EXEC_KEYWORDS = new String[] { "call" };

	public static final String[] DM_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
			BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
			new String[] { "CREATE", "ALTER", "DROP", "ANALYZE", "EXPLAIN" });

	public static final String[][] DM_BEGIN_END_BLOCK = new String[][] {
			{ SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END }, { "IF", SQLConstants.BLOCK_END },
			{ "LOOP", SQLConstants.BLOCK_END + " LOOP" }, { "CASE", SQLConstants.BLOCK_END + " CASE" } };

	public static final String[] DM_BLOCK_HEADERS = new String[] { "DECLARE", "FUNCTION", "PROCEDURE" };

	public static final String[] ADVANCED_KEYWORDS = { "REPLACE", "PACKAGE", "FUNCTION", "TYPE", "TRIGGER",
			"MATERIALIZED", "IF", "EACH", "RETURN", "WRAPPED", "AFTER", "BEFORE", "DATABASE", "ANALYZE", "LOOP",
			"WHILE", "BULK", "ELSIF", "EXIT", "CONTEXT","ADMIN","EXPLAIN"}; // 数据库关键字列表

	private DBPPreferenceStore preferenceStore;

	public DmSQLDialect() {
		super("dm","dm");
	}

	@Override
	public void initDriverSettings(JDBCSession session,JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) { //初始化设置
		super.initDriverSettings(session,dataSource, metaData);
		preferenceStore = dataSource.getContainer().getPreferenceStore();

		addFunctions(Arrays.asList("SUBSTR", "COSH", "SINH", "TANH", "TRUNC", "CHR", "INITCAP", "LPAD", "RPAD",
				"REVERSE", "SUBSTRB", "INSTR", "INSTRB", "LENGTHB", "ADD_MONTHS", "LAST_DAY", "MONTHS_BETWEEN",
				"NEXT_DAY", "SYSDATE", "TO_CHAR", "TRUNC", "HEXTORAW", "RAWTOHEX", "TO_CHAR", "TO_DATE", "TO_NUMBER",
				"SYS_CONNECT_BY_PATH", "PATH", "DECODE", "DUMP", "VSIZE", "NVL", "UID", "FIRST", "LAST", "STDDEV",
				"VARIANCE", "LEAD", "STDDEV", "VARIANCE", "EXTRACT", "OVER"));
		for (String kw : ADVANCED_KEYWORDS) {
			addSQLKeyword(kw);
		}
	}

	public String[][] getBlockBoundStrings() {
		return DM_BEGIN_END_BLOCK;
	}

	public String[] getBlockHeaderStrings() {
		return DM_BLOCK_HEADERS;
	}

	@NotNull
	public String[] getExecuteKeywords() {
		return EXEC_KEYWORDS;
	}

	@NotNull
	public MultiValueInsertMode getMultiValueInsertMode() {
		return MultiValueInsertMode.GROUP_ROWS;
	}

	public boolean supportsAliasInSelect() {
		return true;
	}

	public boolean supportsAliasInUpdate() {
		return true;
	}

	public boolean supportsTableDropCascade() {
		return true;
	}

	public boolean isDelimiterAfterBlock() {
		return true;
	}

	@NotNull
	public DBDBinaryFormatter getNativeBinaryFormatter() {
		return DmBinaryFormatter.INSTANCE;
	}

	@Nullable
	public String getDualTableName() {
		return "DUAL";
	}

	@NotNull
	public String[] getNonTransactionKeywords() {
		return DM_NON_TRANSACTIONAL_KEYWORDS;
	}

	protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
		String schemaName = proc.getParentObject().getName();
		return "CALL " + schemaName + "." + proc.getName();
	}

	public boolean isDisableScriptEscapeProcessing() {
		return preferenceStore == null || preferenceStore.getBoolean("dm.disable.script.escape");
	}

    @NotNull
    @Override
    public String[] getScriptDelimiters() {
        return super.getScriptDelimiters();
    }

}
