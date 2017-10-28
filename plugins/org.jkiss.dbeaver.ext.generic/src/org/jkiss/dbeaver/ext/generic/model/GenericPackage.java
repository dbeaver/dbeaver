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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * GenericPackage
 */
public class GenericPackage extends GenericObjectContainer implements DBPQualifiedObject, GenericScriptObject
{

    private GenericStructContainer container;
    private String packageName;
    private boolean nameFromCatalog;

    public GenericPackage(
        GenericStructContainer container,
        String packageName,
        boolean nameFromCatalog)
    {
        super(container.getDataSource());
        this.container = container;
        this.packageName = packageName;
        this.nameFromCatalog = nameFromCatalog;
        this.procedures = new ArrayList<>();
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return packageName;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return container;
    }

    @Override
    @Property(viewable = true, order = 3)
    public GenericCatalog getCatalog()
    {
        return container.getCatalog();
    }

    @Override
    @Property(viewable = true, order = 4)
    public GenericSchema getSchema()
    {
        return container.getSchema();
    }

    @Override
    public DBSObject getObject()
    {
        return this;
    }

    @Override
    public Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return procedures;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getCatalog(),
            getSchema(),
            this);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return procedures;
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException
    {
        return DBUtils.findObject(procedures, childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return GenericProcedure.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException
    {
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        procedures.clear();
        return this;
    }

    public boolean isNameFromCatalog()
    {
        return nameFromCatalog;
    }

    public void addProcedure(GenericProcedure procedure)
    {
        procedures.add(procedure);
    }

    public void hasProcedure(GenericProcedure procedure)
    {
        procedures.add(procedure);
    }

    public void orderProcedures()
    {
        DBUtils.orderObjects(procedures);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return null;
    }
}
