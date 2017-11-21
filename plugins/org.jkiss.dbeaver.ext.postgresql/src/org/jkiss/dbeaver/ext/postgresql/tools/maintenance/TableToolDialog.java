/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptProgressListener;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptStatusDialog;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * TableToolDialog
 */
public abstract class TableToolDialog extends GenerateMultiSQLDialog<PostgreObject>
{

    public TableToolDialog(IWorkbenchPartSite partSite, String title, Collection<PostgreTableBase> tables) {
        super(partSite, title, toObjects(tables), true);
    }

    public TableToolDialog(IWorkbenchPartSite partSite, String title, PostgreDatabase database) {
        super(partSite, title, Collections.<PostgreObject>singletonList(database), true);
    }

    private static Collection<PostgreObject> toObjects(Collection<PostgreTableBase> tables) {
        List<PostgreObject> objectList = new ArrayList<>();
        objectList.addAll(tables);
        return objectList;
    }

    @Override
    protected SQLScriptProgressListener<PostgreObject> getScriptListener() {
        return new SQLScriptStatusDialog<PostgreObject>(getTitle() + " progress", null) {
            @Override
            protected void createStatusColumns(Tree objectTree) {
                TreeColumn msgColumn = new TreeColumn(objectTree, SWT.NONE);
                msgColumn.setText("Message");
            }

            @Override
            public void processObjectResults(@NotNull PostgreObject object, @Nullable DBCStatement statement, @Nullable DBCResultSet resultSet) throws DBCException {
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
                        if (warnNum == 0) {
                            treeItem.setText(1, "Done");
                        }
                    } catch (SQLException e) {
                        // ignore
                    }
                    treeItem.setExpanded(true);
                }
            }

            @Override
            public void endObjectProcessing(@NotNull PostgreObject object, Exception error) {
                super.endObjectProcessing(object, error);
                if (error != null) {
                    TreeItem treeItem = getTreeItem(object);
                    if (treeItem != null) {
                        treeItem.setText(1, error.getMessage());
                    }
                }
            }
        };
    }
}
