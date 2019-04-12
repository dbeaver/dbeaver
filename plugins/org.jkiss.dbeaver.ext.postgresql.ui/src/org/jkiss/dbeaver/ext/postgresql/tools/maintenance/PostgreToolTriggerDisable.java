package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import java.util.Collection;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTrigger;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.utils.CommonUtils;

public class PostgreToolTriggerDisable implements IExternalTool {
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        List<PostgreTrigger> trigger = CommonUtils.filterCollection(objects, PostgreTrigger.class);
        if (!trigger.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), trigger);
            dialog.open();
        } 
    }

    static class SQLDialog extends TableToolDialog {

        public SQLDialog(IWorkbenchPartSite partSite, List<PostgreTrigger> selectedTrigger)
        {
            super(partSite, "Disable trigger", selectedTrigger);
        }


        @Override
        protected void generateObjectCommand(List<String> lines, PostgreObject object) {
                lines.add("ALTER TABLE " + ((PostgreTrigger)object).getTable() + " DISABLE TRIGGER " + DBUtils.getQuotedIdentifier((PostgreTrigger)object) );
        }

        @Override
        protected void createControls(Composite parent) {
            createObjectsSelector(parent);
        }
    }

}
