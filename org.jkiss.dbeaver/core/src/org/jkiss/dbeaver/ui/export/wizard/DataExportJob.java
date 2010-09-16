/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IResultSetProvider;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * Data export job
 */
public class DataExportJob extends AbstractJob implements DBDDataReceiver {

    private IResultSetProvider dataProvider;
    private DataExportSettings settings;

    public DataExportJob(IResultSetProvider dataProvider, DataExportSettings settings) {
        super("Export data from \"" + dataProvider.getResultSetSource().getName() + "\"");
        this.dataProvider = dataProvider;
        this.settings = settings;

        setUser(true);
    }

    public IResultSetProvider getDataProvider()
    {
        return dataProvider;
    }

    @Override
    public boolean belongsTo(Object family) {
        return family == settings;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {

        DBCExecutionContext context = dataProvider.getDataSource().openIsolatedContext(monitor, "Export data");
        try {

            dataProvider.extractData(context, this, 0, 200000);

        } catch (DBException e) {
            return DBeaverUtils.makeExceptionStatus("Error while exporting data", e);
        } finally {
            context.close();
        }

        return Status.OK_STATUS;
    }

    public void fetchStart(DBRProgressMonitor monitor, DBCResultSet resultSet) throws DBCException {
    }

    public void fetchRow(DBRProgressMonitor monitor, DBCResultSet resultSet) throws DBCException {
    }

    public void fetchEnd(DBRProgressMonitor monitor) throws DBCException {
    }

}
