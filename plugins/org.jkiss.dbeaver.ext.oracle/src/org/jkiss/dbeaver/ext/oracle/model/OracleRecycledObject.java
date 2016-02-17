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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * Recycled object
 */
public class OracleRecycledObject extends OracleSchemaObject implements DBSObjectLazy<OracleDataSource> {

    public enum Operation {
        DROP,
        TRUNCATE
    }

    private String recycledName;
    private Operation operation;
    private String objectType;
    private Object tablespace;
    private String createTime;
    private String dropTime;
    private String partitionName;
    private boolean canUndrop;
    private boolean canPurge;

    protected OracleRecycledObject(OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "ORIGINAL_NAME"), true);
        this.recycledName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        this.operation = CommonUtils.valueOf(Operation.class, JDBCUtils.safeGetString(dbResult, "OPERATION"));
        this.objectType = JDBCUtils.safeGetString(dbResult, "TYPE");
        this.tablespace = JDBCUtils.safeGetString(dbResult, "TS_NAME");
        this.createTime = JDBCUtils.safeGetString(dbResult, "CREATETIME");
        this.dropTime = JDBCUtils.safeGetString(dbResult, "DROPTIME");
        this.partitionName = JDBCUtils.safeGetString(dbResult, "PARTITION_NAME");
        this.canUndrop = JDBCUtils.safeGetBoolean(dbResult, "CAN_UNDROP", "Y");
        this.canPurge = JDBCUtils.safeGetBoolean(dbResult, "CAN_PURGE", "Y");
    }

    @Property(viewable = true, order = 2)
    public String getRecycledName()
    {
        return recycledName;
    }

    @Property(viewable = true, order = 3)
    public Operation getOperation()
    {
        return operation;
    }

    @Property(viewable = true, order = 4)
    public String getObjectType()
    {
        return objectType;
    }

    @Property(viewable = true, order = 5)
    @LazyProperty(cacheValidator = OracleTablespace.TablespaceReferenceValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        return OracleTablespace.resolveTablespaceReference(monitor, this, null);
    }

    @Property(order = 6)
    public String getCreateTime()
    {
        return createTime;
    }

    @Property(order = 7)
    public String getDropTime()
    {
        return dropTime;
    }

    @Property(order = 8)
    public String getPartitionName()
    {
        return partitionName;
    }

    @Property(viewable = true, order = 9)
    public boolean isCanUndrop()
    {
        return canUndrop;
    }

    @Property(viewable = true, order = 10)
    public boolean isCanPurge()
    {
        return canPurge;
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return tablespace;
    }

}
