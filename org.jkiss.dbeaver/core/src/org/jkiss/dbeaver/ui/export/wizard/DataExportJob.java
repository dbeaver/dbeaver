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

    private DataExportSettings settings;

    public DataExportJob(DataExportSettings settings) {
        super("Export data");
        this.settings = settings;

        setUser(true);
    }

    @Override
    public boolean belongsTo(Object family) {
        return family == settings;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {

        for (;;) {
            IResultSetProvider dataProvider = settings.acquireDataProvider();
            if (dataProvider == null) {
                break;
            }
            extractData(monitor, dataProvider);
        }

        return Status.OK_STATUS;
    }

    private void extractData(DBRProgressMonitor monitor, IResultSetProvider dataProvider)
    {
        setName("Export data from \"" + dataProvider.getResultSetSource().getName() + "\"");

        String contextTask = "Export data";
        DBCExecutionContext context = settings.isOpenNewConnections() ?
            dataProvider.getDataSource().openIsolatedContext(monitor, contextTask) :
            dataProvider.getDataSource().openContext(monitor, contextTask);
        try {

            dataProvider.extractData(context, this, 0, 200000);

        } catch (DBException e) {
            new DataExportErrorJob(e).schedule();
        } finally {
            context.close();
        }
    }

    public void fetchStart(DBRProgressMonitor monitor, DBCResultSet resultSet) throws DBCException {
    }

    public void fetchRow(DBRProgressMonitor monitor, DBCResultSet resultSet) throws DBCException {
    }

    public void fetchEnd(DBRProgressMonitor monitor) throws DBCException {
    }

}
