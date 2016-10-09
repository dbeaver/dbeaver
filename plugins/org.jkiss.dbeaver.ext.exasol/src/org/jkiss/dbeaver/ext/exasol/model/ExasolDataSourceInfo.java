package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;

class ExasolDataSourceInfo extends JDBCDataSourceInfo {

    public ExasolDataSourceInfo(JDBCDatabaseMetaData metaData) {
        super(metaData);
    }

    @Override
    public boolean supportsMultipleResults() {
        return false;
    }

}
