/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;

/**
 * DBXTreeIcon
 */
public class DBXTreeIcon
{
    static Log log = LogFactory.getLog(DBXTreeIcon.class);

    private String exprString;
    private Image icon;
    private Expression expr;

    public DBXTreeIcon(String exprString, Image icon)
    {
        this.exprString = exprString;
        this.icon = icon;
        try {
            this.expr = ExpressionFactory.createExpression(exprString);
        } catch (Exception ex) {
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

    public Expression getExpr()
    {
        return expr;
    }

    public Image getIcon()
    {
        return icon;
    }
}
