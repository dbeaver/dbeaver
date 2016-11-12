/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptProgressListener;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptStatusDialog;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Collection;

/**
 * TableToolDialog
 */
public abstract class TableToolDialog extends GenerateMultiSQLDialog<PostgreTable>
{

    public TableToolDialog(IWorkbenchPartSite partSite, String title, Collection<PostgreTable> objects) {
        super(partSite, title, objects, true);
    }

    @Override
    protected SQLScriptProgressListener<PostgreTable> getScriptListener() {
        return new SQLScriptStatusDialog<PostgreTable>(getShell(), getTitle() + " progress", null) {
            @Override
            protected void createStatusColumns(Tree objectTree) {
                TreeColumn msgColumn = new TreeColumn(objectTree, SWT.NONE);
                msgColumn.setText("Message");
            }

            @Override
            public void processObjectResults(@NotNull PostgreTable object, @Nullable DBCStatement statement, @Nullable DBCResultSet resultSet) throws DBCException {
                if (statement == null) {
                    return;
                }
                TreeItem treeItem = getTreeItem(object);
                if (treeItem != null) {
                    try {
                        int warnNum = 0;
                        SQLWarning warning = ((JDBCStatement) statement).getWarnings();
                        while (warning != null) {
                            if (warnNum == 0) {
                                treeItem.setText(1, warning.getMessage());
                            } else {
                                TreeItem warnItem = new TreeItem(treeItem, SWT.NONE);
                                warnItem.setText(0, "");
                                warnItem.setText(1, warning.getMessage());
                            }
                            warnNum++;
                            warning = warning.getNextWarning();
                        }
                    } catch (SQLException e) {
                        // ignore
                    }
                    treeItem.setExpanded(true);
                }
            }
        };
    }
}
