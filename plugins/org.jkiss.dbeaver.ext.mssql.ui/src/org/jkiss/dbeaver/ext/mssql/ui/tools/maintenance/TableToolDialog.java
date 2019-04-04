package org.jkiss.dbeaver.ext.mssql.ui.tools.maintenance;

import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTable;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptProgressListener;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLScriptStatusDialog;

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
        };
    }
}
