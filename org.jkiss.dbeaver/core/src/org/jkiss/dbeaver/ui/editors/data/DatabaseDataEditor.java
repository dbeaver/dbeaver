/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.IResultSetProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

/**
 * DatabaseDataEditor
 */
public class DatabaseDataEditor extends AbstractDatabaseObjectEditor<IDatabaseObjectManager<DBSDataContainer>> implements IResultSetProvider
{
    static final Log log = LogFactory.getLog(DatabaseDataEditor.class);

    private ResultSetViewer resultSetView;
    private boolean loaded = false;
    private boolean running = false;

    public void createPartControl(Composite parent)
    {
        resultSetView = new ResultSetViewer(parent, getSite(), this);
    }

    public void activatePart()
    {
        if (!loaded) {
            resultSetView.refresh();
            loaded = true;
        }
    }

    public void deactivatePart()
    {
/*
        if (curSession != null) {
            try {
                curSession.close();
            } catch (DBCException ex) {
                log.error("Error closing session", ex);
            }
            curSession = null;
        }
*/
    }

    public DBSDataSourceContainer getDataSourceContainer() {
        return getDataContainer().getDataSource().getContainer();
    }

    public DBPDataSource getDataSource() {
        return getDataContainer().getDataSource();
    }

    public DBPNamedObject getResultSetSource() {
        return getDataContainer();
    }

    public boolean isReadyToRun() {
        return getDataSource() != null;
    }

    public DBSDataContainer getDataContainer()
    {
        return getObjectManager().getObject();
    }

    public void extractData(DBCExecutionContext context, DBDDataReceiver dataReceiver, int offset, int maxRows) throws DBException {
        getDataContainer().readData(
            context,
            dataReceiver,
            offset,
            maxRows);
    }

}
