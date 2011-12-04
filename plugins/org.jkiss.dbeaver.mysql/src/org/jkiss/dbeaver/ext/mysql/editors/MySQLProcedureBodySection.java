/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ui.properties.tabbed.SourceEditSection;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * MySQLProcedureBodySection
 */
public class MySQLProcedureBodySection extends SourceEditSection {

    private MySQLProcedure procedure;

    public MySQLProcedureBodySection(IDatabaseEditor editor)
    {
        super(editor);
        this.procedure = (MySQLProcedure) editor.getEditorInput().getDatabaseObject();
    }

    @Override
    protected boolean isReadOnly()
    {
        return false;
    }

    @Override
    protected boolean isSourceRead()
    {
        return true;
    }

    @Override
    protected String loadSources(DBRProgressMonitor monitor) throws DBException
    {
        return procedure.getClientBody(monitor);
    }

    @Override
    protected void updateSources(String source)
    {
        getEditor().getEditorInput().getPropertySource().setPropertyValue("clientBody", source);
    }

}