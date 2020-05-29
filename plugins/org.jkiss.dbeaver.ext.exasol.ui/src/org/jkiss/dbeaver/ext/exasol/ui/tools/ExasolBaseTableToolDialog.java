/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.ui.tools;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.exasol.ui.internal.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableBase;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCStatementImpl;
import org.jkiss.dbeaver.model.impl.local.LocalResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptProgressListener;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptStatusDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class ExasolBaseTableToolDialog
		extends GenerateMultiSQLDialog<ExasolTableBase> {
	
    private static final String VARIABLE_DATE = "date";
    private static final String VARIABLE_TABLE = "table";
    private static final String VARIABLE_SCHEMA = "schema";
	

	ExasolBaseTableToolDialog(IWorkbenchPartSite partSite, String title,
                              Collection<ExasolTableBase> objects)
	{
		super(partSite, title, objects, true);
	}
	
    private int getNumberExtraResultingColumns()
    {
        return 0;
    }
    
    protected String replaceVars(String input, final ExasolTableBase table)
    {
        return GeneralUtils.replaceVariables(input, name -> {
            switch (name) {
                case VARIABLE_TABLE:
                    return table.getName();
                case VARIABLE_SCHEMA:
                    return table.getContainer().getName();
                case VARIABLE_DATE:
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Calendar cal = Calendar.getInstance();
                    return dateFormat.format(cal.getTime());
                 default:
                    System.getProperty(name);
            }
            return null;
        });

    }
    
	
	@Override
	protected SQLScriptProgressListener<ExasolTableBase> getScriptListener()
	{
        final int nbExtraColumns = getNumberExtraResultingColumns();

        return new SQLScriptStatusDialog<ExasolTableBase>(getTitle() + " " + ExasolMessages.dialog_table_tools_progress,null) {
        	@Override
        	protected void createStatusColumns(Tree objectTree)
        	{
                TreeColumn msgColumn = new TreeColumn(objectTree, SWT.NONE);
                msgColumn.setText(ExasolMessages.dialog_table_tools_result);

                for (int i = 0; i < nbExtraColumns; i++) {
                    new TreeColumn(objectTree, SWT.NONE);
                }
        	}
        	
            // DF: This method is for tools that return resultsets
            @Override
            public void processObjectResults(@NotNull ExasolTableBase exasolTable, @Nullable DBCStatement statement, @Nullable DBCResultSet resultSet) throws DBCException
            {
                if (resultSet == null) {
                    return;
                }
                // Retrieve column names
            	DBCResultSetMetaData rsMetaData = resultSet.getMeta();

                try {

                    TreeItem treeItem = getTreeItem(exasolTable);
                    Font f = UIUtils.makeBoldFont(treeItem.getFont());
                    if (treeItem != null) {

                        // Display the column names
                        TreeItem subItem = null;
                        subItem = new TreeItem(treeItem, SWT.NONE);
                        subItem.setFont(f);
                        for (DBCAttributeMetaData column: rsMetaData.getAttributes()) {
                            subItem.setText(column.getOrdinalPosition(), column.getName());
                            subItem.setGrayed(true);
                        }

                        // Display the data for each row
                        while (resultSet.nextRow()) {
                            subItem = new TreeItem(treeItem, SWT.NONE);
                            for (int i = 0; i < rsMetaData.getAttributes().size(); i++) {
                                subItem.setText(i, CommonUtils.toString(resultSet.getAttributeValue(i)));
                                i++;
                            }
                        }
                        treeItem.setExpanded(true);
                    }
                } catch (Exception e) {
                    throw new DBCException(e.getMessage());
                }

            }

        
        	
        };
	}
	
    @Override
    protected void executeSQL() {
        final String jobName = getShell().getText();
        final SQLScriptProgressListener<ExasolTableBase> scriptListener = getScriptListener();
        final List<ExasolTableBase> objects = getCheckedObjects();
        final Map<ExasolTableBase, List<String>> objectsSQL = new LinkedHashMap<>();
        for (ExasolTableBase object : objects) {
            final List<String> lines = new ArrayList<>();
            generateObjectCommand(lines, object);
            objectsSQL.put(object, lines);
        }
        final DataSourceJob job = new DataSourceJob(jobName, getExecutionContext()) {
            Exception objectProcessingError;

            @SuppressWarnings("rawtypes")
			@Override
            protected IStatus run(final DBRProgressMonitor monitor)
            {
                final DataSourceJob curJob = this;
                UIUtils.asyncExec(() -> scriptListener.beginScriptProcessing(curJob, objects));
                monitor.beginTask(jobName, objects.size());
                try (DBCSession session = getExecutionContext().openSession(monitor, DBCExecutionPurpose.UTIL, jobName)) {
                    for (int i = 0; i < objects.size(); i++) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        final int objectNumber = i;
                        final ExasolTableBase object = objects.get(i);
                        monitor.subTask("Process " + DBUtils.getObjectFullName(object, DBPEvaluationContext.UI));
                        objectProcessingError = null;
                        UIUtils.asyncExec(() -> scriptListener.beginObjectProcessing(object, objectNumber));
                        try {
                            final List<String> lines = objectsSQL.get(object);
                            for (String line : lines) {
                                try (final Statement statement = ((JDBCSession) session).getOriginal().createStatement()) {
                                	int affectedRows = statement.executeUpdate(line);
                                	
                                	Integer[] resultSetData = new Integer[] { affectedRows };
                                    	
                                	final LocalResultSet resultSet = new LocalResultSet<>(session, new JDBCStatementImpl<>((JDBCSession) session, statement, true));
                                	resultSet.addColumn("ROWS_AFFECTED", DBPDataKind.NUMERIC);
                                	resultSet.addRow((Object[]) resultSetData );
                                	
                                    // Run in sync because we need result set
                                    UIUtils.syncExec(() -> {
                                            try {
                                                scriptListener.processObjectResults(object, null, resultSet);
                                            } catch (DBCException e) {
                                                objectProcessingError = e;
                                            }
                                    });
                                    if (objectProcessingError != null) {
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            objectProcessingError = e;
                        } finally {
                            UIUtils.asyncExec(() -> scriptListener.endObjectProcessing(object, objectProcessingError));
                        }
                        monitor.worked(1);
                    }
                } finally {
                    monitor.done();
                    UIUtils.asyncExec(scriptListener::endScriptProcessing);
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(false);
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
            }
        });
        job.schedule();
    }

}
