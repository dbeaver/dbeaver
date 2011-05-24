/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * MySQLProcedureBodySection
 */
public class MySQLProcedureBodySection extends SourceEditSection {

    private MySQLProcedure procedure;

    public MySQLProcedureBodySection(IDatabaseNodeEditor editor)
    {
        super(editor);
        this.procedure = (MySQLProcedure) editor.getEditorInput().getDatabaseObject();
    }

    @Override
    protected boolean isReadOnly()
    {
        return true;
    }

    @Override
    protected boolean isSourceRead()
    {
        return true;
    }

    @Override
    protected String loadSources(DBRProgressMonitor monitor) throws DBException
    {
        return procedure.getBody();
    }

}