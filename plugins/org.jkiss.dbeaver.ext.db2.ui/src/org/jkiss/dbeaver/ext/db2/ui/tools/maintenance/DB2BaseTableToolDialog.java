/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.ui.tools.maintenance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.ui.internal.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetMetaDataImpl;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptProgressListener;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptStatusDialog;
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

        return new SQLScriptStatusDialog<DB2Table>(getTitle() + " " + DB2Messages.dialog_table_tools_progress, null) {
            @Override
            protected void createStatusColumns(Tree objectTree)
            {
                TreeColumn msgColumn = new TreeColumn(objectTree, SWT.NONE);
                msgColumn.setText(DB2Messages.dialog_table_tools_result);

                for (int i = 0; i < nbExtraColumns; i++) {
                    new TreeColumn(objectTree, SWT.NONE);
                }
            }

            // DF: This method is for tools that return resultsets
            @Override
            public void processObjectResults(@NotNull DB2Table db2Table, @Nullable DBCStatement statement, @Nullable DBCResultSet resultSet) throws DBCException
            {
                if (resultSet == null) {
                    return;
                }
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
