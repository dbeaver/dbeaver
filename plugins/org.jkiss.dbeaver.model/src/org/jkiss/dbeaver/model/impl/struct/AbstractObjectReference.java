/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
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
    private final String extraInfo;

    protected AbstractObjectReference(String name, DBSObject container, String description, Class<?> objectClass, DBSObjectType type) {
        this(name, container, description, objectClass, type, null);
    }

    protected AbstractObjectReference(String name, DBSObject container, String description, Class<?> objectClass, DBSObjectType type, String extraInfo)
    {
        this.name = name;
        this.container = container;
        this.description = description;
        this.objectClass = objectClass;
        this.type = type;
        this.extraInfo = extraInfo;
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
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        if (extraInfo != null) {
            return extraInfo;
        }
        String fqName;
        DBPDataSource dataSource = container.getDataSource();
        if (container == dataSource) {
            // In case if there are no schemas/catalogs supported
            // and data source is a root container
            fqName = DBUtils.getQuotedIdentifier(dataSource, name);
        } else {
            fqName = DBUtils.getFullQualifiedName(dataSource, container, this);
        }
        return fqName;
    }

    @Override
    public String toString() {
        return getFullyQualifiedName(DBPEvaluationContext.UI);
    }
}
