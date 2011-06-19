/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedureStandalone;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.properties.tabbed.SourceEditSection;

/**
 * OracleProcedureBodySection
 */
public class OracleProcedureBodySection extends SourceEditSection {

    private OracleProcedureStandalone procedure;

    public OracleProcedureBodySection(IDatabaseNodeEditor editor)
    {
        super(editor);
        this.procedure = (OracleProcedureStandalone) editor.getEditorInput().getDatabaseObject();
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
        return "";
    }

}