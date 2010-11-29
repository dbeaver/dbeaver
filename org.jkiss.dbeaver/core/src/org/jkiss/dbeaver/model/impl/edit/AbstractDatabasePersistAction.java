/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;

/**
 * Object persist action implementation
 */
public class AbstractDatabasePersistAction implements IDatabasePersistAction {

    private final String title;
    private final String script;
    private final String undoScript;

    public AbstractDatabasePersistAction(String title, String script, String undoScript)
    {
        this.title = title;
        this.script = script;
        this.undoScript = undoScript;
    }

    public AbstractDatabasePersistAction(String title, String script)
    {
        this(title, script, null);
    }

    public AbstractDatabasePersistAction(String script)
    {
        this("", script, null);
    }

    public String getTitle()
    {
        return title;
    }

    public String getScript()
    {
        return script;
    }

    public String getUndoScript()
    {
        return undoScript;
    }

}
