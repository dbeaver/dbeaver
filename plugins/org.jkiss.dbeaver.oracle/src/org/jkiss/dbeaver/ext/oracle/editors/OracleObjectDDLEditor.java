/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * OracleObjectDDLEditor
 */
public class OracleObjectDDLEditor extends OracleSourceAbstractEditor<OracleTable> {

    @Override
    public boolean isReadOnly()
    {
        return true;
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        return ((OracleTable)getEditorInput().getDatabaseObject()).getDDL(monitor);
    }

    @Override
    protected void setSourceText(String sourceText) {
    }

}