/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.tools.maintenance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptStatusDialog;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptProgressListener;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Table truncate
 */
public abstract class TableToolDialog extends GenerateMultiSQLDialog<MySQLTable>
{

    public TableToolDialog(IWorkbenchPartSite partSite, String title, Collection<MySQLTable> objects) {
        super(partSite, title, objects, true);
    }

    @Override
    protected SQLScriptProgressListener<MySQLTable> getScriptListener() {
        return new SQLScriptStatusDialog<MySQLTable>(getTitle() + " progress", null) {
            @Override
            protected void createStatusColumns(Tree objectTree) {
                TreeColumn msgColumn = new TreeColumn(objectTree, SWT.NONE);
                msgColumn.setText("Message");
            }

            @Override
            public void processObjectResults(@NotNull MySQLTable object, @Nullable DBCStatement statement, @Nullable DBCResultSet resultSet) throws DBCException {
                if (resultSet == null) {
                    return;
                }
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
