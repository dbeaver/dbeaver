package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

public abstract class PostgreForkTable extends PostgreTableRegular {
    public PostgreForkTable(PostgreSchema catalog) {
        super(catalog);
    }

    public PostgreForkTable(PostgreSchema catalog, ResultSet dbResult) {
        super(catalog, dbResult);
    }

    @Override
    public boolean supportsChangingReferentialIntegrity(@NotNull DBRProgressMonitor monitor) {
        return false;
    }

    @Override
    public void setReferentialIntegrity(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        throw new DBException("Changing referential integrity is not supported");
    }

    @NotNull
    @Override
    public String getCaveatsDescription(@NotNull DBRProgressMonitor monitor) {
        return "";
    }
}
