package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBConstants;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureStandalone;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableBase;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBSourceObject;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBSourceDeclarationEditor extends SQLSourceViewer<YashanDBSourceObject> implements YashanDBDDLOptions {
    private Map<String, Object> yashandbDDLOptions = new HashMap<>();

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
        getInputPropertySource().setPropertyValue(
                monitor,
                YashanDBConstants.PROP_OBJECT_DEFINITION,
                sourceText);
    }

    @Override
    protected boolean isReadOnly() {
        return false;
    }

    @Override
    protected void contributeEditorCommands(IContributionManager toolBarManager) {
        super.contributeEditorCommands(toolBarManager);

        if (getSourceObject() instanceof YashanDBTableBase) {
            YashanDBTableBase sourceObject = (YashanDBTableBase) getSourceObject();
            YashanDBEditorUtils.addDDLControl(toolBarManager, sourceObject, this);
            toolBarManager.add(new Separator());
            toolBarManager.add(ActionUtils.makeActionContribution(
                    new Action("Show header", Action.AS_CHECK_BOX) {
                        {
                            setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TREE_PROCEDURE));
                            setToolTipText("Shows auto-generated function header");
                            setChecked(!isInDebugMode());
                        }
                        @Override
                        public void run() {
                            getDatabaseEditorInput().setAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE, !isChecked());
                            refreshPart(YashanDBSourceDeclarationEditor.this, true);
                        }
                    }, true
            ));
        }
    }

    @Override
    public void putDDLOptions(String name, Object value) {
        yashandbDDLOptions.put(name, value);
    }

    @Override
    protected Map<String, Object> getSourceOptions() {
        Map<String, Object> options = super.getSourceOptions();
        if (!CommonUtils.isEmpty(yashandbDDLOptions)) {
            options.putAll(yashandbDDLOptions);
        }
        options.put(DBPScriptObject.OPTION_DEBUGGER_SOURCE,isInDebugMode());
        return options;
    }

    private boolean isInDebugMode(){
        return CommonUtils.getBoolean(
                getDatabaseEditorInput().getAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE), false);
    }

    @Override
    protected boolean isAnnotationRulerVisible() {
        return getSourceObject() instanceof YashanDBProcedureStandalone;
    }
}
