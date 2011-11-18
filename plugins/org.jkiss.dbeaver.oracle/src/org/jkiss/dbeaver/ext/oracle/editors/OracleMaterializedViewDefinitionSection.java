/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.properties.tabbed.SourceEditSection;

/**
 * OracleMaterializedViewDefinitionSection
 */
public class OracleMaterializedViewDefinitionSection extends SourceEditSection {

    private OracleMaterializedView view;

    public OracleMaterializedViewDefinitionSection(IDatabaseNodeEditor editor)
    {
        super(editor);
        this.view = (OracleMaterializedView) editor.getEditorInput().getDatabaseObject();
    }

    @Override
    protected boolean isSourceRead()
    {
        return true;
    }

    @Override
    protected String loadSources(DBRProgressMonitor monitor) throws DBException
    {
        return view.getSourceDeclaration(monitor);
    }

    protected void updateSources(String source)
    {
        getEditor().getEditorInput().getPropertySource().setPropertyValue("declaration", source);
    }

}