/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
