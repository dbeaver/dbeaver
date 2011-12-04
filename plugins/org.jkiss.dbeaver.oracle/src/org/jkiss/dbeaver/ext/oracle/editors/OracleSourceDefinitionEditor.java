/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObjectEx;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Oracle source definition editor
 */
public class OracleSourceDefinitionEditor extends OracleSourceAbstractEditor<OracleSourceObjectEx> {


    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        return getSourceObject().getSourceDefinition(monitor);
    }

    @Override
    protected void setSourceText(String sourceText) {
        getEditorInput().getPropertySource().setPropertyValue(
            OracleConstants.PROP_SOURCE_DEFINITION, sourceText);
    }

}
