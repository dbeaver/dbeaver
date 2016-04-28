/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.navigator.meta;

import org.apache.commons.jexl2.Expression;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

/**
 * DBXTreeIcon
 */
public class DBXTreeIcon
{
    private static final Log log = Log.getLog(DBXTreeIcon.class);

    private String exprString;
    private DBPImage icon;
    private Expression expression;

    public DBXTreeIcon(String exprString, DBPImage icon)
    {
        this.exprString = exprString;
        this.icon = icon;
        try {
            this.expression = AbstractDescriptor.parseExpression(exprString);
        } catch (DBException ex) {
            log.warn("Can't parse icon expression: " + exprString, ex);
        }
    }

    public void dispose()
    {
    }

    public String getExprString()
    {
        return exprString;
    }

    public Expression getExpression()
    {
        return expression;
    }

    public DBPImage getIcon()
    {
        return icon;
    }
}
