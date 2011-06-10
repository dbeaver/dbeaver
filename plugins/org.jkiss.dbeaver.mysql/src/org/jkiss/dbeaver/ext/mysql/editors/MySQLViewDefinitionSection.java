/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.mysql.model.MySQLView;
import org.jkiss.dbeaver.ui.properties.tabbed.SourceEditSection;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * MySQLViewDefinitionSection
 */
public class MySQLViewDefinitionSection extends SourceEditSection {

    private MySQLView view;

    public MySQLViewDefinitionSection(IDatabaseNodeEditor editor)
    {
        super(editor);
        this.view = (MySQLView) editor.getEditorInput().getDatabaseObject();
    }

    @Override
    protected boolean isSourceRead()
    {
        return view.getAdditionalInfo().isLoaded();
    }

    @Override
    protected String loadSources(DBRProgressMonitor monitor) throws DBException
    {
        return view.getAdditionalInfo(monitor).getDefinition();
    }

    protected void updateSources(String source)
    {
        getEditor().getEditorInput().getPropertySource().setPropertyValue("definition", source);
    }

}