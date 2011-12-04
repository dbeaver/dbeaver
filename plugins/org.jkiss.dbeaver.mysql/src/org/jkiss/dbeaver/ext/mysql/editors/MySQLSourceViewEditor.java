/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLSourceObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseTextEditor;

/**
 * MySQLSourceViewEditor
 */
public class MySQLSourceViewEditor extends AbstractDatabaseTextEditor<MySQLSourceObject> {

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