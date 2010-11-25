/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.swt.graphics.Image;

/**
 * Object change action
 */
public class DatabaseObjectChangeAction {

    private final String script;
    private final String description;
    private final Image icon;
    private final boolean required;

    public DatabaseObjectChangeAction(String script, String description, Image icon, boolean required)
    {
        this.script = script;
        this.description = description;
        this.icon = icon;
        this.required = required;
    }

    public DatabaseObjectChangeAction(String script, String description, boolean required)
    {
        this(script, description, null, required);
    }

    public DatabaseObjectChangeAction(String script, String description)
    {
        this(script, description, null, true);
    }

    public DatabaseObjectChangeAction(String script)
    {
        this(script, null, null, true);
    }

    public String getScript()
    {
        return script;
    }

    public String getDescription()
    {
        return description;
    }

    public Image getIcon()
    {
        return icon;
    }

    public boolean isRequired()
    {
        return required;
    }
}
