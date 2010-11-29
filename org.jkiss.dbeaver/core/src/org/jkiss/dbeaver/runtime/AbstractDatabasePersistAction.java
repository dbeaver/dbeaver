/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;

/**
 * Object persist action implementation
 */
public class AbstractDatabasePersistAction implements IDatabasePersistAction {

    private final String script;
    private final String title;
    private final Image icon;

    public AbstractDatabasePersistAction(String script, String title, Image icon)
    {
        this.script = script;
        this.title = title;
        this.icon = icon;
    }

    public AbstractDatabasePersistAction(String script, String title)
    {
        this(script, title, null);
    }

    public AbstractDatabasePersistAction(String script)
    {
        this(script, null, null);
    }

    public String getTitle()
    {
        return title;
    }

    public Image getIcon()
    {
        return icon;
    }

    public String getScript()
    {
        return script;
    }

    public String getUndoScript()
    {
        return null;
    }
}
