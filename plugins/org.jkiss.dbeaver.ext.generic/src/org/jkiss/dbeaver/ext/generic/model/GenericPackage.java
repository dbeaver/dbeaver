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
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException {
        return null;
    }
}
