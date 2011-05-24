/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * MySQLTableDDLSection
 */
public class MySQLTableDDLSection extends SourceEditSection {

    private MySQLTable table;

    public MySQLTableDDLSection(IDatabaseNodeEditor editor)
    {
        super(editor);
        this.table = (MySQLTable) editor.getEditorInput().getDatabaseObject();
    }

    @Override
    protected boolean isReadOnly()
    {
        return true;
    }

    @Override
    protected boolean isSourceRead()
    {
        return false;
    }

    @Override
    protected String loadSources(DBRProgressMonitor monitor) throws DBException
    {
        return table.getDDL(monitor);
    }

}