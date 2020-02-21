package org.jkiss.dbeaver.ext.mssql.ui.tools.maintenance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerObject;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptProgressListener;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptStatusDialog;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class TableToolDialog extends GenerateMultiSQLDialog<SQLServerObject> {

    public TableToolDialog(IWorkbenchPartSite partSite, String title, Collection<? extends SQLServerObject> tables) {
        super(partSite, title, toObjects(tables), true);
    }
    public TableToolDialog(IWorkbenchPartSite partSite, String title, SQLServerTable database) {
        super(partSite, title, Collections.<SQLServerObject>singletonList(database), true);
    }
    private static Collection<SQLServerObject> toObjects(Collection<? extends SQLServerObject> tables) {
        List<SQLServerObject> objectList = new ArrayList<>();
        objectList.addAll(tables);
        return objectList;
    }



    @Override
    protected SQLScriptProgressListener<SQLServerObject> getScriptListener() {
        return new SQLScriptStatusDialog<SQLServerObject>(getTitle() + " progress", null) {
            @Override
            protected void createStatusColumns(Tree objectTree) {
                TreeColumn msgColumn = new TreeColumn(objectTree, SWT.NONE);
                msgColumn.setText("Message");
            }
            @Override
            public void processObjectResults(@NotNull SQLServerObject object, @Nullable DBCStatement statement, @Nullable DBCResultSet resultSet) throws DBCException {
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
        };
    }
}
