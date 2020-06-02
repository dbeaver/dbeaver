/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.data.AbstractDataEditor;

import java.util.ResourceBundle;

/**
 * CursorViewComposite
 */
public class CursorViewComposite extends Composite implements IResultSetContainer {

    private static final Log log = Log.getLog(CursorViewComposite.class);

    private IValueController valueController;
    private DBDCursor value;
    private DBCResultSet resultSet;
    private ResultSetViewer resultSetViewer;
    private CursorDataContainer dataContainer;
    private boolean fetched;

    public CursorViewComposite(Composite parent, IValueController valueController) {
        super(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        this.setLayout(layout);
        this.valueController = valueController;
        dataContainer = new CursorDataContainer();

        value = (DBDCursor) valueController.getValue();

        if (value != null) {
            DBPPreferenceStore globalPreferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
            if (!globalPreferenceStore.getBoolean(ResultSetPreferences.KEEP_STATEMENT_OPEN)) {
                if (ConfirmationDialog.showConfirmDialog(
                        ResourceBundle.getBundle(ResultSetMessages.BUNDLE_NAME),
                        getShell(),
                        ResultSetPreferences.CONFIRM_KEEP_STATEMENT_OPEN,
                        ConfirmationDialog.QUESTION) == IDialogConstants.YES_ID)
                {
                    globalPreferenceStore.setValue(ResultSetPreferences.KEEP_STATEMENT_OPEN, true);
                    if (valueController.getValueSite().getPart() instanceof IResultSetContainer) {
                        IResultSetController rsv = ((IResultSetContainer) valueController.getValueSite().getPart()).getResultSetController();
                        if (rsv != null) {
                            rsv.refresh();
                        }
                    }
                } else {
                    // No value
                }
            }
        }

        resultSetViewer = new ResultSetViewer(this, valueController.getValueSite(), this);

        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.grabExcessVerticalSpace = true;
        resultSetViewer.getControl().setLayoutData(gd);

        //resultSetViewer.refresh();
    }

    public void setValue(DBDCursor value) {
        if (this.value != value) {
            this.fetched = false;
            this.value = value;
        }
    }

    @Nullable
    @Override
    public DBPProject getProject() {
        return valueController.getExecutionContext().getDataSource().getContainer().getProject();
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return valueController == null ? null : valueController.getExecutionContext();
    }

    @Nullable
    @Override
    public ResultSetViewer getResultSetController()
    {
        return resultSetViewer;
    }

    @Nullable
    @Override
    public DBSDataContainer getDataContainer()
    {
        return dataContainer;
    }

    @Override
    public boolean isReadyToRun()
    {
        return true;
    }

    @Override
    public void openNewContainer(DBRProgressMonitor monitor, @NotNull DBSDataContainer dataContainer, @NotNull DBDDataFilter newFilter) {
        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null) {
            return;
        }
        final DBNDatabaseNode targetNode = executionContext.getDataSource().getContainer().getPlatform().getNavigatorModel().getNodeByObject(monitor, dataContainer, false);
        if (targetNode == null) {
            UIUtils.showMessageBox(null, "Open link", "Can't navigate to '" + DBUtils.getObjectFullName(dataContainer, DBPEvaluationContext.UI) + "' - navigator node not found", SWT.ICON_ERROR);
            return;
        }
        AbstractDataEditor.openNewDataEditor(targetNode, newFilter);

    }

    @Override
    public IResultSetDecorator createResultSetDecorator() {
        return new QueryResultsDecorator() {
            @Override
            public long getDecoratorFeatures() {
                return valueController.getEditType() == IValueController.EditType.EDITOR ?
                    FEATURE_PANELS | FEATURE_PRESENTATIONS | FEATURE_STATUS_BAR | FEATURE_LINKS :
                    FEATURE_PRESENTATIONS;
            }
        };
    }

    public Control getControl() {
        return resultSetViewer == null ? null : resultSetViewer.getControl();
    }

    public void refresh() {
        // Refresh only once.
        // Cursor contents cannot change because it lives within current transaction
        if (!fetched) {
            resultSetViewer.refresh();
            fetched = true;
        }
    }

    public boolean isDirty() {
        return resultSetViewer.isDirty();
    }

    private class CursorDataContainer implements DBSDataContainer {

        @Override
        public int getSupportedFeatures()
        {
            // Nothing but plain read
            return DATA_SELECT;
        }

        @NotNull
        @Override
        public DBCStatistics readData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags, int fetchSize) throws DBCException
        {
            DBCStatistics statistics = new DBCStatistics();
            resultSet = value == null ? null : value.openResultSet(session);
            if (resultSet == null) {
                return statistics;
            }
            DBRProgressMonitor monitor = session.getProgressMonitor();
            if (firstRow > 0) {
                try {
                    resultSet.moveTo((int) firstRow);
                } catch (DBCException e) {
                    log.debug(e);
                }
            }
            try {
                long startTime = System.currentTimeMillis();
                dataReceiver.fetchStart(session, resultSet, firstRow, maxRows);
                long rowCount;
                try {
                    rowCount = 0;
                    while (resultSet.nextRow()) {
                        if (monitor.isCanceled()) {
                            // Fetch not more than max rows
                            break;
                        }
                        dataReceiver.fetchRow(session, resultSet);
                        rowCount++;
                        if (rowCount >= maxRows) {
                            break;
                        }
                        if (rowCount % 100 == 0) {
                            monitor.subTask(rowCount + ResultSetMessages.dialog_cursor_view_monitor_rows_fetched);
                            monitor.worked(100);
                        }

                    }
                } finally {
                    try {
                        dataReceiver.fetchEnd(session, resultSet);
                    } catch (DBCException e) {
                        log.error("Error while finishing result set fetch", e); //$NON-NLS-1$
                    }
                }
                statistics.setFetchTime(System.currentTimeMillis() - startTime);
                statistics.setRowsFetched(rowCount);
                return statistics;
            }
            finally {
                dataReceiver.close();
            }
        }

        @Override
        public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, DBDDataFilter dataFilter, long flags)
        {
            return -1;
        }

        @Nullable
        @Override
        public String getDescription()
        {
            return value.toString();
        }

        @Override
        public DBSObject getParentObject()
        {
            return null;
        }

        @Nullable
        @Override
        public DBPDataSource getDataSource()
        {
            return valueController == null ? null : valueController.getExecutionContext().getDataSource();
        }

        @NotNull
        @Override
        public String getName()
        {
            return value.toString();
        }

        @Override
        public boolean isPersisted()
        {
            return false;
        }
    }

}
