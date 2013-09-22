/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * Pseudo attribute
 */
public class DBDPseudoAttribute implements DBPNamedObject {

    private final DBDPseudoAttributeType type;
    private final String name;
    private final String queryExpression;
    private final String alias;
    private final String description;

    public DBDPseudoAttribute(DBDPseudoAttributeType type, String name, String queryExpression, String alias, String description)
    {
        this.type = type;
        this.name = name;
        this.queryExpression = queryExpression;
        this.alias = alias;
        this.description = description;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public DBDPseudoAttributeType getType()
    {
        return type;
    }

    public String getQueryExpression()
    {
        return queryExpression;
    }

    public String getAlias()
    {
        return alias;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public String toString()
    {
        return name + " (" + type + ")";
    }
}
