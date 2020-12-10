package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDInsertReplaceMethod;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

public class FireBirdInsertReplaceMethod implements DBDInsertReplaceMethod {

    @NotNull
    @Override
    public String getOpeningClause(DBSTable table, DBRProgressMonitor monitor) {
        return "UPDATE OR INSERT INTO";
    }

    @Override
    public String getTrailingClause(DBSTable table, DBRProgressMonitor monitor, DBSAttributeBase[] attributes) {
        return null;
    }
}
