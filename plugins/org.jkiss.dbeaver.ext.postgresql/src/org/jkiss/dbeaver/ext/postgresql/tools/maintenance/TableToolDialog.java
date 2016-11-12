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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptStatusDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptProgressListener;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Table truncate
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
            public void processObjectResults(PostgreTable object, DBCResultSet resultSet) throws DBCException {
                Map<String, String> statusMap = new LinkedHashMap<>();
                while (resultSet.nextRow()) {
                    statusMap.put(
                        CommonUtils.toString(resultSet.getAttributeValue("Msg_type")),
                        CommonUtils.toString(resultSet.getAttributeValue("Msg_text")));
                }
                TreeItem treeItem = getTreeItem(object);
                if (treeItem != null && !statusMap.isEmpty()) {
                    if (statusMap.size() == 1) {
                        treeItem.setText(1, statusMap.values().iterator().next());
                    } else {
                        String statusText = statusMap.get("status");
                        if (!CommonUtils.isEmpty(statusText)) {
                            treeItem.setText(1, statusText);
                        }
                        for (Map.Entry<String, String> status : statusMap.entrySet()) {
                            if (!status.getKey().equals("status")) {
                                TreeItem subItem = new TreeItem(treeItem, SWT.NONE);
                                subItem.setText(0, status.getKey());
                                subItem.setText(1, status.getValue());
                            }
                        }
                        treeItem.setExpanded(true);
                    }
                }
            }
        };
    }
}
