/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;

/**
 * Object persist action implementation
 */
public class AbstractDatabasePersistAction implements IDatabasePersistAction {

    private final String title;
    private final String script;
    private final ActionType type;

    public AbstractDatabasePersistAction(String title, String script)
    {
        this(title, script, ActionType.NORMAL);
    }

    public AbstractDatabasePersistAction(String title, String script, ActionType type)
    {
        this.title = title;
        this.script = script;
        this.type = type;
    }

    public AbstractDatabasePersistAction(String script)
    {
        this("", script, ActionType.NORMAL);
    }

    public String getTitle()
    {
        return title;
    }

    public String getScript()
    {
        return script;
    }

    public void handleExecute(Throwable error)
    {
        // do nothing
    }

    public ActionType getType()
    {
        return type;
    }

}
