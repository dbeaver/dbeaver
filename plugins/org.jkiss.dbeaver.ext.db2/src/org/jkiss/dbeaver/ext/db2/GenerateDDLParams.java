package org.jkiss.dbeaver.ext.db2;

import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.*;

public class GenerateDDLParams {

    private final String statementDelimiter;
    private final DB2DataSource dataSource;
    private final DB2Table db2Table;
    private final boolean includeViews;

    public GenerateDDLParams(String statementDelimiter, DB2DataSource dataSource, DB2Table db2Table, boolean includeViews) {
        this.statementDelimiter = statementDelimiter;
        this.dataSource = dataSource;
        this.db2Table = db2Table;
        this.includeViews = includeViews;
    }

    public String getStatementDelimiter() {
        return statementDelimiter;
    }

    public DB2DataSource getDataSource() {
        return dataSource;
    }

    public DB2Table getDb2Table() {
        return db2Table;
    }

    public boolean isIncludeViews() {
        return includeViews;
    }
}
