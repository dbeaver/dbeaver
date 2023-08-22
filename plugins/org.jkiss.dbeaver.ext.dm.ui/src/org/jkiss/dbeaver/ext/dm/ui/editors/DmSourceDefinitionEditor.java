package org.jkiss.dbeaver.ext.dm.ui.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * DM source definition editor
 * @author caosw
 *
 */
public class DmSourceDefinitionEditor extends SQLSourceViewer<DmSourceObject> {

	@Override
	protected String getCompileCommandId() {
		return DmConstants.CMD_COMPILE;
	}

	@Override
	protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
		return ((DBPScriptObjectExt)getSourceObject()).getExtendedDefinitionText(monitor);
	}

	@Override
	protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
		getInputPropertySource().setPropertyValue(monitor, DmConstants.PROP_OBJECT_BODY_DEFINITION, sourceText);
	}

	@Override
    protected boolean isReadOnly() {
        return false;
    }
	
}
