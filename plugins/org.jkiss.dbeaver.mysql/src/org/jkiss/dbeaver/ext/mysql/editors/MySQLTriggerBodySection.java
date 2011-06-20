/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTrigger;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.properties.tabbed.SourceEditSection;

/**
 * MySQLTriggerBodySection
 */
public class MySQLTriggerBodySection extends SourceEditSection {

    private MySQLTrigger trigger;

    public MySQLTriggerBodySection(IDatabaseNodeEditor editor)
    {
        super(editor);
        this.trigger = (MySQLTrigger) editor.getEditorInput().getDatabaseObject();
    }

    @Override
    protected boolean isSourceRead()
    {
        return true;
    }

    @Override
    protected String loadSources(DBRProgressMonitor monitor) throws DBException
    {
        return trigger.getBody();
    }

    @Override
    protected boolean isReadOnly()
    {
        return true;
    }
}