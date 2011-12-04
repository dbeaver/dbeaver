/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.oracle.model.OracleView;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.properties.tabbed.SourceEditSection;

/**
 * OracleViewDefinitionSection
 */
public class OracleViewDefinitionSection extends SourceEditSection {

    private OracleView view;

    public OracleViewDefinitionSection(IDatabaseEditor editor)
    {
        super(editor);
        this.view = (OracleView) editor.getEditorInput().getDatabaseObject();
    }

    @Override
    protected boolean isSourceRead()
    {
        return view.getAdditionalInfo().isLoaded();
    }

    @Override
    protected String loadSources(DBRProgressMonitor monitor) throws DBException
    {
        return view.getAdditionalInfo(monitor).getText();
    }

    protected void updateSources(String source)
    {
        getEditor().getEditorInput().getPropertySource().setPropertyValue("text", source);
    }

}