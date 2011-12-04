/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Oracle source declaration editor
 */
public class OracleSourceDeclarationEditor extends OracleSourceAbstractEditor<OracleSourceObject> {


    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        return getSourceObject().getSourceDeclaration(monitor);
    }

    @Override
    protected void setSourceText(String sourceText) {
        getEditorInput().getPropertySource().setPropertyValue(
            OracleConstants.PROP_SOURCE_DECLARATION, sourceText);
    }

}
