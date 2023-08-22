package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class YashanDBDataBase extends JDBCRemoteInstance {
    public YashanDBDataBase(JDBCDataSource dataSource) {
        super(dataSource);
    }

    @NotNull
    @Override
    public  YashanDBExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose, @Nullable DBCExecutionContext initFrom) throws DBException {
        YashanDBExecutionContext ec = (YashanDBExecutionContext) super.openIsolatedContext(monitor, purpose, initFrom);
        ec.setIsolatedContext(true);
        return ec;
    }
}
