/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.editors.DB2StatefulObject;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableIndexCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2Nickname;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * Super class for DB2 Tables, Views, Nicknames
 * 
 * @author Denis Forveille
 */
public abstract class DB2TableBase extends JDBCTable<DB2DataSource, DB2Schema>
    implements DBPNamedObject2, DBPRefreshableObject, DB2StatefulObject {

    private DB2TableIndexCache tableIndexCache = new DB2TableIndexCache();

    private String owner;
    private DB2OwnerType ownerType;

    private Integer tableId;

    private Timestamp createTime;

    private String remarks;

    // -----------------
    // Constructors
    // -----------------
    public DB2TableBase(DBRProgressMonitor monitor, DB2Schema schema, ResultSet dbResult)
    {
        super(schema, true);

        setName(JDBCUtils.safeGetString(dbResult, "TABNAME"));

        DB2DataSource db2DataSource = schema.getDataSource();

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.tableId = JDBCUtils.safeGetInteger(dbResult, "TABLEID");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");

        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        if (db2DataSource.isAtLeastV9_5()) {
            this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        }
    }

    public DB2TableBase(DB2Schema container, String name, Boolean persisted)
    {
        super(container, name, persisted);
    }

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        tableIndexCache.clearCache();

        // DF: Clear base index cache. Not cheap but didn't found another way..
        getContainer().getIndexCache().clearCache();

        return true;
    }

    // -----------------
    // Columns
    // -----------------

    @Override
    public Collection<DB2TableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        if (this instanceof DB2Table) {
            return getContainer().getTableCache().getChildren(monitor, getContainer(), (DB2Table) this);
        }
        if (this instanceof DB2Nickname) {
            return getContainer().getNicknameCache().getChildren(monitor, getContainer(), (DB2Nickname) this);
        }
        if (this instanceof DB2MaterializedQueryTable) {
            return getContainer().getMaterializedQueryTableCache().getChildren(monitor, getContainer(),
                (DB2MaterializedQueryTable) this);
        }
        if (this instanceof DB2View) {
            return getContainer().getViewCache().getChildren(monitor, getContainer(), (DB2View) this);
        }

        // Other kinds don't have columns..
        throw new DBException("Unknown object with columns encountered");
    }

    @Override
    public DB2TableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException
    {
        if (this instanceof DB2Table) {
            return getContainer().getTableCache().getChild(monitor, getContainer(), (DB2Table) this, attributeName);
        }
        if (this instanceof DB2Nickname) {
            return getContainer().getNicknameCache().getChild(monitor, getContainer(), (DB2Nickname) this, attributeName);
        }
        if (this instanceof DB2MaterializedQueryTable) {
            return getContainer().getMaterializedQueryTableCache().getChild(monitor, getContainer(),
                (DB2MaterializedQueryTable) this, attributeName);
        }
        if (this instanceof DB2View) {
            return getContainer().getViewCache().getChild(monitor, getContainer(), (DB2View) this, attributeName);
        }

        // Other kinds don't have columns..
        throw new DBException("Unknown object with columns encountered");
    }

    // -----------------
    // Associations
    // -----------------
    @Override
    @Association
    public Collection<DB2Index> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return tableIndexCache.getAllObjects(monitor, this);
    }

    // -----------------
    // Associations (Imposed from DBSTable). In DB2, Most of objects "derived"
    // from Tables don't have those..
    // -----------------

    @Nullable
    @Override
    public Collection<DB2TableUniqueKey> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<DB2TableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<DB2TableReference> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return Collections.emptyList();
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Override
    @Property(viewable = true, editable = false, order = 2)
    public DB2Schema getSchema()
    {
        return super.getContainer();
    }

    @Property(viewable = false, editable = false, order = 100, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = true, order = 98)
    public Integer getTableId()
    {
        return tableId;
    }

    @Nullable
    @Override
    @Property(viewable = false, order = 99, editable = true, updatable = true)
    public String getDescription()
    {
        return remarks;
    }

    public void setDescription(String description)
    {
        this.remarks = description;
    }

}
