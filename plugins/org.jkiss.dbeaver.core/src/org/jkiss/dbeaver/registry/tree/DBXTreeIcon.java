/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

/**
 * DBXTreeIcon
 */
public class DBXTreeIcon
{
    static final Log log = LogFactory.getLog(DBXTreeIcon.class);

    private String exprString;
    private Image icon;
    private Expression expression;

    public DBXTreeIcon(String exprString, Image icon)
    {
        this.exprString = exprString;
        this.icon = icon;
        try {
            this.expression = RuntimeUtils.parseExpression(exprString);
        } catch (DBException ex) {
            log.warn("Can't parse icon expression: " + exprString, ex);
        }
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

    public Expression getExpression()
    {
        return expression;
    }

    public Image getIcon()
    {
        return icon;
    }
}
