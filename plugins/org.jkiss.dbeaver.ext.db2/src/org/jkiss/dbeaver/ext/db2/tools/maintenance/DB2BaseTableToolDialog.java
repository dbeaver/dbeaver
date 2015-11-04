/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.tools.maintenance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetMetaDataImpl;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptProgressListener;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptStatusDialog;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Base Dialog for DB2 Tools Dialogs
 */
public abstract class DB2BaseTableToolDialog extends GenerateMultiSQLDialog<DB2Table> {

    public DB2BaseTableToolDialog(IWorkbenchPartSite partSite, String title, Collection<DB2Table> objects)
    {
        super(partSite, title, objects, true);
    }

    protected int getNumberExtraResultingColumns()
    {
        return 0;
    }

    @Override
    protected SQLScriptProgressListener<DB2Table> getScriptListener()
    {
        final int nbExtraColumns = getNumberExtraResultingColumns();

        return new SQLScriptStatusDialog<DB2Table>(getShell(), getTitle() + " " + DB2Messages.dialog_table_tools_progress, null) {
            @Override
            protected void createStatusColumns(Tree objectTree)
            {
                TreeColumn msgColumn = new TreeColumn(objectTree, SWT.NONE);
                msgColumn.setText(DB2Messages.dialog_table_tools_result);

                for (int i = 0; i < nbExtraColumns; i++) {
                    new TreeColumn(objectTree, SWT.NONE);
                }
            }

            @Override
            public void endObjectProcessing(DB2Table db2Table, Exception exception)
            {
                TreeItem treeItem = getTreeItem(db2Table);
                if (exception == null) {
                    treeItem.setText(1, DB2Messages.dialog_table_tools_success_title);
                } else {
                    treeItem.setText(1, exception.getMessage());
                }
                UIUtils.packColumns(treeItem.getParent(), false, null);
            }

            // DF: This method is for tools that return resultsets
            @Override
            public void processObjectResults(DB2Table db2Table, DBCResultSet resultSet) throws DBCException
            {
                // Retrive column names
                JDBCResultSetMetaDataImpl rsMetaData = (JDBCResultSetMetaDataImpl) resultSet.getMeta();

                try {

                    TreeItem treeItem = getTreeItem(db2Table);
                    Font f = UIUtils.makeBoldFont(treeItem.getFont());
                    if (treeItem != null) {

                        // Display the column names
                        TreeItem subItem = null;
                        subItem = new TreeItem(treeItem, SWT.NONE);
                        subItem.setFont(f);
                        for (int i = 0; i < rsMetaData.getColumnCount(); i++) {
                            subItem.setText(i, rsMetaData.getColumnName(i + 1));
                            subItem.setGrayed(true);
                        }

                        // Display the data for each row
                        while (resultSet.nextRow()) {
                            subItem = new TreeItem(treeItem, SWT.NONE);
                            for (int i = 0; i < rsMetaData.getColumnCount(); i++) {
                                subItem.setText(i, CommonUtils.toString(resultSet.getAttributeValue(i)));
                            }
                        }
                        treeItem.setExpanded(true);
                    }
                } catch (SQLException e) {
                    throw new DBCException(e.getMessage());
                }

            }
        };
    }
}
