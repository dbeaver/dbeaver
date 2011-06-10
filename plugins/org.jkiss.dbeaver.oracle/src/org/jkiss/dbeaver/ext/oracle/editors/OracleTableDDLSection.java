/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.properties.tabbed.SourceEditSection;

/**
 * OracleTableDDLSection
 */
public class OracleTableDDLSection extends SourceEditSection {

    private OracleTable table;

    public OracleTableDDLSection(IDatabaseNodeEditor editor)
    {
        super(editor);
        this.table = (OracleTable) editor.getEditorInput().getDatabaseObject();
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