package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTable;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.utils.CommonUtils;

public class YashanDBObjectDDLEditor extends SQLSourceViewer<YashanDBTable> implements YashanDBDDLOptions {

	private Map<String, Object> ddlOptions = new HashMap<>();

	@Override
	protected void contributeEditorCommands(IContributionManager contributionManager) {
		super.contributeEditorCommands(contributionManager);
		if (getSourceObject() instanceof YashanDBTable) {
			YashanDBEditorUtils.addDDLControl(contributionManager, getSourceObject(), this);
		}
	}

	@Override
	public void putDDLOptions(String name, Object value) {
		ddlOptions.put(name, value);
	}

	@Override
	protected Map<String, Object> getSourceOptions() {
		Map<String, Object> options = super.getSourceOptions();
		if (!CommonUtils.isEmpty(ddlOptions)) {
			options.putAll(ddlOptions);
		}
		return options;
	}
}
