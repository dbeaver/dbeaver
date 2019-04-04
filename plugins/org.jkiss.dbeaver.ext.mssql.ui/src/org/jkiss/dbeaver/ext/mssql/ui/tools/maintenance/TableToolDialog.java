package org.jkiss.dbeaver.ext.mssql.ui.tools.maintenance;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptProgressListener;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptStatusDialog;
import org.jkiss.utils.CommonUtils;

public abstract class TableToolDialog extends GenerateMultiSQLDialog<SQLServerTable> {

    public TableToolDialog(IWorkbenchPartSite partSite, String title, Collection<SQLServerTable> objects) {
        super(partSite, title, objects, true);
    }

    @Override
    protected SQLScriptProgressListener<SQLServerTable> getScriptListener() {
        return new SQLScriptStatusDialog<SQLServerTable>(getTitle() + " progress", null) {
            @Override
            protected void createStatusColumns(Tree objectTree) {
                TreeColumn msgColumn = new TreeColumn(objectTree, SWT.NONE);
                msgColumn.setText("Message");
            }

            @Override
            public void processObjectResults(@NotNull SQLServerTable object, @Nullable DBCStatement statement,
                    @Nullable DBCResultSet resultSet) throws DBCException {
                if (resultSet == null) {
                    return;
                }
                Map<String, String> statusMap = new LinkedHashMap<>();
                while (resultSet.nextRow()) {
                    statusMap.put(CommonUtils.toString(resultSet.getAttributeValue("Msg_type")),
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
