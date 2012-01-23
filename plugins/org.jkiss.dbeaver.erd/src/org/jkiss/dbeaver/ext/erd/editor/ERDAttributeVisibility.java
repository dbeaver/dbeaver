/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

/**
 * Entity attribute visibility
 */
public enum ERDAttributeVisibility
{
    ALL("All"),
    KEYS("Any keys"),
    PRIMARY("Primary key"),
    NONE("None");

    private final String title;

    ERDAttributeVisibility(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }
}
