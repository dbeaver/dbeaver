/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLSourceObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorNested;

/**
 * MySQLSourceViewEditor
 */
public class MySQLSourceViewEditor extends SQLEditorNested<MySQLSourceObject> {

    public MySQLSourceViewEditor()
    {
    }

    @Override
    protected boolean isReadOnly()
    {
        return false;
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException
    {
        return getSourceObject().getSourceText(monitor);
    }

    @Override
    protected void setSourceText(String sourceText)
    {
        getEditorInput().getPropertySource().setPropertyValue("sourceText", sourceText);
    }

}