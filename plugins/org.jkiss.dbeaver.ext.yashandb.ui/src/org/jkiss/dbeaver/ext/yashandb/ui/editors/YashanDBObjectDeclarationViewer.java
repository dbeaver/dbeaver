package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableBase;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

public class YashanDBObjectDeclarationViewer extends SQLSourceViewer {

	@Override
	protected boolean isReadOnly() {
		return true;
	}

	@Override
	protected void contributeEditorCommands(IContributionManager toolBarManager) {
		super.contributeEditorCommands(toolBarManager);
		DBSObject sourceObject = getSourceObject();
		if (sourceObject instanceof YashanDBTableBase || sourceObject instanceof YashanDBSchema) {
			YashanDBEditorUtils.addDDLControl(toolBarManager, (YashanDBTableBase) sourceObject, this);
		}
	}
}
