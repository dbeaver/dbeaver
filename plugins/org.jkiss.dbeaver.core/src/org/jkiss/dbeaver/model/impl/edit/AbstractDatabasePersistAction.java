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

    public AbstractDatabasePersistAction(String title, String script)
    {
        this.title = title;
        this.script = script;
    }

    public AbstractDatabasePersistAction(String script)
    {
        this("", script);
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

}
