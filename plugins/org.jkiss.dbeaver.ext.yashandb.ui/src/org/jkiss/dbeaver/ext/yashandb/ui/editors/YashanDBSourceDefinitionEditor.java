package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBConstants;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBSourceObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBSourceDefinitionEditor extends SQLSourceViewer<YashanDBSourceObject> {

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        return ((DBPScriptObjectExt) getSourceObject()).getExtendedDefinitionText(monitor);
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
        getInputPropertySource().setPropertyValue(
                monitor,
                YashanDBConstants.PROP_OBJECT_BODY_DEFINITION,
                sourceText);
    }

    @Override
    protected boolean isReadOnly() {
        return false;
    }
}
