/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.exasol.tools.maintenance;

import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableBase;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCStatementImpl;
import org.jkiss.dbeaver.model.impl.local.LocalResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptProgressListener;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptStatusDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

public abstract class ExasolBaseTableToolDialog
		extends GenerateMultiSQLDialog<ExasolTableBase> {
	
    public static final String VARIABLE_DATE = "date";
    public static final String VARIABLE_TABLE = "table";
    public static final String VARIABLE_SCHEMA = "schema";
	

	public ExasolBaseTableToolDialog(IWorkbenchPartSite partSite, String title,
			Collection<ExasolTableBase> objects)
	{
		super(partSite, title, objects, true);
	}
	
    protected int getNumberExtraResultingColumns()
    {
        return 0;
    }
    
    protected String replaceVars(String input, ExasolTableBase table) 
    {
        String outString = GeneralUtils.replaceVariables(input, new GeneralUtils.IVariableResolver() {
            @Override
            public String get(String name) {
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
            }
        });
        return outString;

    }
    
	
	@Override
	protected SQLScriptProgressListener<ExasolTableBase> getScriptListener()
	{
        final int nbExtraColumns = getNumberExtraResultingColumns();

        return new SQLScriptStatusDialog<ExasolTableBase>(getShell(), getTitle() + " " + ExasolMessages.dialog_table_tools_progress,null) {
        	@Override
        	protected void createStatusColumns(Tree objectTree)
        	{
                TreeColumn msgColumn = new TreeColumn(objectTree, SWT.NONE);
                msgColumn.setText(ExasolMessages.dialog_table_tools_result);

                for (int i = 0; i < nbExtraColumns; i++) {
                    new TreeColumn(objectTree, SWT.NONE);
                }
        	}
        	
            @Override
            public void endObjectProcessing(ExasolTableBase exasolTable, Exception exception)
            {
                TreeItem treeItem = getTreeItem(exasolTable);
                if (exception == null) {
                    treeItem.setText(1, ExasolMessages.dialog_table_tools_success_title);
                } else {
                    treeItem.setText(1, exception.getMessage());
                }
                UIUtils.packColumns(treeItem.getParent(), false, null);
            }

            // DF: This method is for tools that return resultsets
            @Override
            public void processObjectResults(ExasolTableBase exasolTable, DBCResultSet resultSet) throws DBCException
            {
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
        final DataSourceJob job = new DataSourceJob(jobName, null, getExecutionContext()) {
            public Exception objectProcessingError;

            @SuppressWarnings("rawtypes")
			@Override
            protected IStatus run(final DBRProgressMonitor monitor)
            {
                final DataSourceJob curJob = this;
                DBeaverUI.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        scriptListener.beginScriptProcessing(curJob, objects);
                    }
                });
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
                        DBeaverUI.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                scriptListener.beginObjectProcessing(object, objectNumber);
                            }
                        });
                        try {
                            final List<String> lines = objectsSQL.get(object);
                            for (String line : lines) {
                                try (Statement statement = ((JDBCSession) session).getOriginal().createStatement()) {
                                	int affectedRows = statement.executeUpdate(line);
                                	
                                	Integer[] resultSetData = new Integer[] { affectedRows };
                                    	
                                	LocalResultSet resultSet = new LocalResultSet<JDBCStatement>(session, (JDBCStatement) new JDBCStatementImpl<Statement>((JDBCSession) session, statement, true));
                                	resultSet.addColumn("ROWS_AFFECTED", DBPDataKind.NUMERIC);
                                	resultSet.addRow((Object[]) resultSetData );
                                	
                                    // Run in sync because we need result set
                                    DBeaverUI.syncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                                try {
													scriptListener.processObjectResults(object, resultSet);
												} catch (DBCException e) {
													objectProcessingError = e;
												}
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
                            DBeaverUI.asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    scriptListener.endObjectProcessing(object, objectProcessingError);
                                }
                            });
                        }
                        monitor.worked(1);
                    }
                } finally {
                    monitor.done();
                    DBeaverUI.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            scriptListener.endScriptProcessing();
                        }
                    });
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
