package org.jkiss.dbeaver.ext.dm.ui.editors;

import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

public class DmSourceDeclarationEditor extends SQLSourceViewer<DmSourceObject> {

	@Override
	protected String getCompileCommandId() {
		return DmConstants.CMD_COMPILE;
	}

	@Override
	protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
		getInputPropertySource().setPropertyValue(monitor, DmConstants.PROP_OBJECT_DEFINITION, sourceText);
	}

	@Override
	protected boolean isReadOnly() {
		return false;
	}
	
}
