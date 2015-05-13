/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.registry.tree;

import org.apache.commons.jexl2.Expression;
import org.jkiss.dbeaver.core.Log;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

/**
 * DBXTreeIcon
 */
public class DBXTreeIcon
{
    static final Log log = Log.getLog(DBXTreeIcon.class);

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
