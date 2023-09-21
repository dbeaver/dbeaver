package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraintColumn;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
public class YashanDBTableSortKeyColumn extends AbstractTableConstraintColumn {

    public YashanDBTableSortKeyColumn(DBRProgressMonitor monitor, YashanDBTableSortKey sortKey, YashanDBTableColumn tableColumn, JDBCResultSet resultSet) {
        super();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBPDataSource getDataSource() {
        return null;
    }

    @Override
    public DBSTableConstraint getParentObject() {
        return null;
    }

    @Override
    public DBSTableColumn getAttribute() {
        return null;
    }

    @Override
    public int getOrdinalPosition() {
        return 0;
    }
}
