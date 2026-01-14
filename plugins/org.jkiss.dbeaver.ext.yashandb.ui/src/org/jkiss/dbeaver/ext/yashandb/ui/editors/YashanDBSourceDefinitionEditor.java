package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSourceObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

public class YashanDBSourceDefinitionEditor extends SQLSourceViewer<YashanDBSourceObject> {

	@Override
	protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
		return ((DBPScriptObjectExt) getSourceObject()).getExtendedDefinitionText(monitor);
	}

	@Override
	protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
		getInputPropertySource().setPropertyValue(monitor, "extendedDefinitionText", sourceText);
	}

	@Override
	protected boolean isReadOnly() {
		return false;
	}
}
