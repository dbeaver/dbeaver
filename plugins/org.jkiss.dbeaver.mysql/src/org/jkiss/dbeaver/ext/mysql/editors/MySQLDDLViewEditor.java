/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseTextEditor;

/**
 * MySQLDDLViewEditor
 */
public class MySQLDDLViewEditor extends AbstractDatabaseTextEditor<MySQLTable> {

    @Override
    protected boolean isReadOnly()
    {
        return true;
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException
    {
        return getSourceObject().getDDL(monitor);
    }

    @Override
    protected void setSourceText(String sourceText)
    {
    }

}