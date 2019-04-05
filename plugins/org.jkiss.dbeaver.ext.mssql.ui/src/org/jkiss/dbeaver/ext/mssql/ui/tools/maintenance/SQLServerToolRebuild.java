package org.jkiss.dbeaver.ext.mssql.ui.tools.maintenance;

import java.util.Collection;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTable;
import org.jkiss.dbeaver.ext.mssql.ui.tools.maintenance.TableToolDialog;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.utils.CommonUtils;

public class SQLServerToolRebuild implements IExternalTool {
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
            throws DBException {
        List<SQLServerTable> tables = CommonUtils.filterCollection(objects, SQLServerTable.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        }
    }

    static class SQLDialog extends TableToolDialog {
        public SQLDialog(IWorkbenchPartSite partSite, Collection<SQLServerTable> selectedTables) {
            super(partSite, "Rebuild index(s)", selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, SQLServerTable object) {
            lines.add("ALTER INDEX ALL ON " + object.getFullyQualifiedName(DBPEvaluationContext.DDL) + " REBUILD ");
        }

        @Override
        protected void createControls(Composite parent) {
            createObjectsSelector(parent);
        }
    }

}
