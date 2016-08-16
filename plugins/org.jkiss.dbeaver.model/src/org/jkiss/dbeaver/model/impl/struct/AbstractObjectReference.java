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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

/**
 * Abstract object reference
 */
public abstract class AbstractObjectReference implements DBSObjectReference {

    private final String name;
    private final DBSObject container;
    private final String description;
    private final Class<?> objectClass;
    private final DBSObjectType type;

    protected AbstractObjectReference(String name, DBSObject container, String description, Class<?> objectClass, DBSObjectType type)
    {
        this.name = name;
        this.container = container;
        this.description = description;
        this.objectClass = objectClass;
        this.type = type;
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    public DBSObject getContainer()
    {
        return container;
    }

    @Override
    public Class<?> getObjectClass() {
        return objectClass;
    }

    @Override
    public String getObjectDescription()
    {
        return description;
    }

    @Override
    public DBSObjectType getObjectType()
    {
        return type;
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        DBPDataSource dataSource = container.getDataSource();
        if (container == dataSource) {
            // In case if there are no schemas/catalogs supported
            // and data source is a root container
            return DBUtils.getQuotedIdentifier(dataSource, name);
        }
        return DBUtils.getFullQualifiedName(dataSource, container, this);

    }

    @Override
    public String toString() {
        return getFullQualifiedName();
    }
}
