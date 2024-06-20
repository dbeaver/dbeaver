package org.jkiss.dbeaver.ext.gaussdb.ui.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBPackage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreScriptObject;
import org.jkiss.dbeaver.ext.postgresql.ui.editors.PostgreSourceViewEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class GaussDBPackageBodyViewEditor extends PostgreSourceViewEditor {

    @Override
    protected boolean isReadOnly() {
        return false;
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
        getInputPropertySource().setPropertyValue(monitor, "extendedDefinitionText", sourceText);
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        PostgreScriptObject object = getSourceObject();
        if (object instanceof GaussDBPackage) {
            GaussDBPackage sourceObject = (GaussDBPackage) object;
            return sourceObject.getExtendedDefinitionText();
        }
        return "";
    }
}