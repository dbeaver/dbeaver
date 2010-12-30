/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;

/**
 * DBXTreeIcon
 */
public class DBXTreeIcon
{
    static final Log log = LogFactory.getLog(DBXTreeIcon.class);

    private String exprString;
    private Image icon;

    public DBXTreeIcon(String exprString, Image icon)
    {
        this.exprString = exprString;
        this.icon = icon;
    }

    public void dispose()
    {
        if (icon != null) {
            icon.dispose();
        }
    }

    public String getExprString()
    {
        return exprString;
    }

    public Image getIcon()
    {
        return icon;
    }
}
