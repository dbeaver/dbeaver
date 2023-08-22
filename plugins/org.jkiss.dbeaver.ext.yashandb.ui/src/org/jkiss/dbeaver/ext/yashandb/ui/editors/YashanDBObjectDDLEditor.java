package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTable;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBObjectDDLEditor extends SQLSourceViewer<YashanDBTable> implements YashanDBDDLOptions {
    private Map<String, Object> yashandbDDLOptions = new HashMap<>();

    public YashanDBObjectDDLEditor() {
    }

    @Override
    protected void contributeEditorCommands(IContributionManager contributionManager) {
        super.contributeEditorCommands(contributionManager);
        YashanDBTable sourceObject = getSourceObject();
        YashanDBEditorUtils.addDDLControl(contributionManager, sourceObject, this);
    }

    public void putDDLOptions(String name, Object value) {
        yashandbDDLOptions.put(name, value);
    }

    @Override
    protected Map<String, Object> getSourceOptions() {
        Map<String, Object> options = super.getSourceOptions();
        if (!CommonUtils.isEmpty(yashandbDDLOptions)) {
            options.putAll(yashandbDDLOptions);
        }
        return options;
    }

}
