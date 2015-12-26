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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * GenericSequence
 */
public class PostgreSequence implements PostgreClass, DBSSequence, DBPQualifiedObject
{
    private PostgreSchema schema;
    private int oid;
    private String name;
    private Number lastValue;
    private Number minValue;
    private Number maxValue;
    private Number incrementBy;

    public PostgreSequence(PostgreSchema schema, ResultSet dbResult) {
        this.schema = schema;
        this.oid = JDBCUtils.safeGetInt(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "relname");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public int getObjectId() {
        return this.oid;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 10)
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public PostgreSchema getParentObject() {
        return schema;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return schema.getDataSource();
    }

    @NotNull
    @Override
    public String getFullQualifiedName() {
        return DBUtils.getFullQualifiedName(getDataSource(),
            schema.getDatabase(),
            schema,
            this);
    }

    @Override
    @Property(viewable = true, order = 2)
    public Number getLastValue() {
        return lastValue;
    }

    public void setLastValue(Number lastValue) {
        this.lastValue = lastValue;
    }

    @Override
    @Property(viewable = true, order = 3)
    public Number getMinValue() {
        return minValue;
    }

    @Override
    @Property(viewable = true, order = 4)
    public Number getMaxValue() {
        return maxValue;
    }

    @Override
    @Property(viewable = true, order = 5)
    public Number getIncrementBy() {
        return incrementBy;
    }

    ///////////////////////////////////////////////////////////////////////
    // Entity

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return false;
    }

    @Override
    public DBSEntityType getEntityType() {
        return PostgreConstants.ENTITY_TYPE_SEQENCE;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException {
        return null;
    }
}
