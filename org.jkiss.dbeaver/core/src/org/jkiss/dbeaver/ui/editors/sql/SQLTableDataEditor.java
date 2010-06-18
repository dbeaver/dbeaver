/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.ui.IEmbeddedWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.runtime.sql.DefaultQueryListener;
import org.jkiss.dbeaver.runtime.sql.SQLQueryJob;
import org.jkiss.dbeaver.runtime.sql.SQLQueryResult;
import org.jkiss.dbeaver.runtime.sql.SQLStatementInfo;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.util.Collections;

/**
 * SQLTableData
 */
public class SQLTableDataEditor extends EditorPart implements IEmbeddedWorkbenchPart, IMetaModelView, ResultSetProvider
{
    static Log log = LogFactory.getLog(SQLTableDataEditor.class);

    private ResultSetViewer resultSetView;
    private DBMModel model;
    private DBSTable table;

    public void doSave(IProgressMonitor monitor)
    {
    }

    public void doSaveAs()
    {
    }

    public void init(IEditorSite site, IEditorInput input)
            throws PartInitException {
        setSite(site);
        setInput(input);
    }

    public void dispose()
    {
        super.dispose();
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void createPartControl(Composite parent)
    {
        resultSetView = new ResultSetViewer(parent, getSite(), this);
    }

    public void setFocus()
    {
    }

    public void activatePart()
    {
        if (table == null) {
            IEditorInput editorInput = getEditorInput();
            if (!(editorInput instanceof IDatabaseEditorInput)) {
                log.error("Table data editor must be used only with databse editor input!");
                return;
            }
            IDatabaseEditorInput dbei = (IDatabaseEditorInput)editorInput;
            model = dbei.getModel();
            DBSObject dbmObject = dbei.getDatabaseObject();
            if (!(dbmObject instanceof DBSTable)) {
                log.error("Table data editor must be used only with databse editor input!");
                return;
            }
            table = (DBSTable)dbmObject;

            resultSetView.refresh();
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

    public DBMModel getMetaModel()
    {
        return model;
    }

    public Viewer getViewer()
    {
        return resultSetView;
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

    public DBSDataSourceContainer getDataSourceContainer() {
        return table.getDataSource().getContainer();
    }

    public DBPDataSource getDataSource() {
        return table.getDataSource();
    }

    public boolean isConnected()
    {
        return table != null && table.getDataSource().getContainer().isConnected();
    }

    public void extractResultSetData(int offset)
    {
        if (!isConnected()) {
            DBeaverUtils.showErrorDialog(getSite().getShell(), "Not Connected", "Not Connected");
            return;
        }
        log.debug("Extract table data");
        StringBuilder query = new StringBuilder();
        String tableName = table.getFullQualifiedName();
        query.append("SELECT * FROM ").append(tableName);
        SQLStatementInfo statementInfo = new SQLStatementInfo(query.toString());
        final SQLQueryJob job = new SQLQueryJob(
            "Table " + tableName,
            getDataSource(),
            Collections.singletonList(statementInfo),
            resultSetView.getDataPump());
        job.setDataContainer(table);

        job.addQueryListener(new DefaultQueryListener()
        {
            public void onEndQuery(final SQLQueryResult result)
            {
                getSite().getShell().getDisplay().asyncExec(new Runnable() {
                    public void run()
                    {
                        if (result.getError() != null) {
                            resultSetView.setStatus(result.getError().getMessage(), true);
                            log.error(result.getError().getMessage());
                        } else if (result.getRowCount() != null) {
                            resultSetView.setStatus(result.getRowCount() + " row(s)", false);
                        } else {
                            resultSetView.setStatus("Empty resultset", false);
                        }
                    }
                });
            }
        });
        job.schedule();
    }

}