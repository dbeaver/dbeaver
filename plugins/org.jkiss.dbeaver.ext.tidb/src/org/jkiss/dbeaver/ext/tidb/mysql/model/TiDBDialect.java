package org.jkiss.dbeaver.ext.tidb.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDialect;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;

public class TiDBDialect extends MySQLDialect {
    public static final String[] TIDB_NON_TRANSACTIONAL_KEYWORDS = new String[]{ "OPTIMISTIC", "PESSIMISTIC" };
    
    private static final String[] TIDB_ADVANCED_KEYWORDS = {
        "AUTO_RANDOM",
        "PLACEMENT",
        "POLICY",
        "REORGANIZE",
        "EXCHANGE",
        "CACHE",
        "NONCLUSTERED",
        "CLUSTERED"
    };
    
    private static final String[] TIDB_EXTRA_FUNCTIONS = {
        "TIDB_BOUNDED_STALENESS",
        "TIDB_DECODE_KEY",
        "TIDB_DECODE_PLAN",
        "TIDB_IS_DDL_OWNER",
        "TIDB_PARSE_TSO",
        "TIDB_VERSION",
        "TIDB_DECODE_SQL_DIGESTS",
        "TIDB_SHARD"
    };
    
    public TiDBDialect() {
        super("TiDB", "tidb");
    }
    
    @Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initBaseDriverSettings(session, dataSource, metaData);
        
        for (String kw : TIDB_ADVANCED_KEYWORDS) {
            addSQLKeyword(kw);
        }
        for (String kw : TIDB_NON_TRANSACTIONAL_KEYWORDS) {
            addSQLKeyword(kw);
        }
        addFunctions(Arrays.asList(TIDB_EXTRA_FUNCTIONS));
    }
    
    @NotNull
    public String[] getNonTransactionKeywords() {
        return ArrayUtils.concatArrays(
            MySQLDialect.MYSQL_NON_TRANSACTIONAL_KEYWORDS,
            TIDB_NON_TRANSACTIONAL_KEYWORDS
        );
    }
    
    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        // TiDB do not support the stored procedures
        return new String[]{};
    }
}
