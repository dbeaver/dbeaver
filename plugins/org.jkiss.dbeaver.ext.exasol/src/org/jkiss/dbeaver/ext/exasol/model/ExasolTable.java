/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolSourceObject;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * @author Karl
 */
public class ExasolTable extends ExasolTableBase implements DBPRefreshableObject, DBPNamedObject2, ExasolSourceObject {

    private Boolean hasDistKey;
    private Timestamp lastCommit;

    private long sizeRaw;

    private long sizeCompressed;

    private float deletePercentage;

    private Timestamp createTime;

    public ExasolTable(DBRProgressMonitor monitor, ExasolSchema schema, ResultSet dbResult) {
        super(monitor, schema, dbResult);
        this.hasDistKey = JDBCUtils.safeGetBoolean(dbResult, "TABLE_HAS_DISTRIBUTION_KEY");
        this.lastCommit = JDBCUtils.safeGetTimestamp(dbResult, "LAST_COMMIT");
        this.sizeRaw = JDBCUtils.safeGetLong(dbResult, "RAW_OBJECT_SIZE");
        this.sizeCompressed = JDBCUtils.safeGetLong(dbResult, "MEM_OBJECT_SIZE");
        this.deletePercentage = JDBCUtils.safeGetFloat(dbResult, "DELETE_PERCENTAGE");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");

    }

    public ExasolTable(ExasolSchema schema, String name) {
        super(schema, name, false);
    }


    // -----------------
    // Properties
    // -----------------
    @Property(viewable = false, editable = false, order = 90, category = ExasolConstants.CAT_BASEOBJECT)
    public Boolean getHasDistKey() {
        return hasDistKey;
    }

    @Property(viewable = false, editable = false, order = 100, category = ExasolConstants.CAT_BASEOBJECT)
    public Timestamp getLastCommit() {
        return lastCommit;
    }

    @Property(viewable = false, editable = false, order = 100, category = ExasolConstants.CAT_DATETIME)
    public Timestamp getCreateTime() {
        return createTime;
    }

    @Property(viewable = false, editable = false, order = 150, category = ExasolConstants.CAT_STATS)
    public String getRawsize() {
        return ExasolUtils.humanReadableByteCount(sizeRaw, true);
    }

    @Property(viewable = false, editable = false, order = 200, category = ExasolConstants.CAT_STATS)
    public String getCompressedsize() {
        return ExasolUtils.humanReadableByteCount(sizeCompressed, true);
    }

    @Property(viewable = false, editable = false, order = 250, category = ExasolConstants.CAT_STATS)
    public float getDeletePercentage() {
        return this.deletePercentage;
    }


    // -----------------
    // Associations
    // -----------------
    @Nullable
    @Override
    @Association
    public Collection<ExasolTableUniqueKey> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().getConstraintCache().getObjects(monitor, getContainer(), this);
    }

    public ExasolTableUniqueKey getConstraint(DBRProgressMonitor monitor, String ukName) throws DBException {
        return getContainer().getConstraintCache().getObject(monitor, getContainer(), this, ukName);
    }

    @Override
    @Association
    public Collection<ExasolTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().getAssociationCache().getObjects(monitor, getContainer(), this);
    }

    public DBSTableForeignKey getAssociation(DBRProgressMonitor monitor, String ukName) throws DBException {
        return getContainer().getAssociationCache().getObject(monitor, getContainer(), this, ukName);
    }

    // -----------------
    // Business Contract
    // -----------------
    @Override
    public boolean isView() {
        return false;
    }

    @Override
    public JDBCStructCache<ExasolSchema, ExasolTable, ExasolTableColumn> getCache() {
        return getContainer().getTableCache();
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        getContainer().getTableCache().clearChildrenCache(this);

        getContainer().getConstraintCache().clearObjectCache(this);
        getContainer().getAssociationCache().clearObjectCache(this);

        return this;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException {
        return ExasolUtils.generateDDLforTable(monitor, this.getDataSource(), this);
    }

    @Override
    public DBSObjectState getObjectState() {
        // table can only be in state normal
        return DBSObjectState.NORMAL;
    }

}
