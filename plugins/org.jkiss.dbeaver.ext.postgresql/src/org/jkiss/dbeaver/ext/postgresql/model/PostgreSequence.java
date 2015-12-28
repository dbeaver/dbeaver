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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

import java.util.Collection;

/**
 * PostgreSequence
 */
public class PostgreSequence implements PostgreClass, DBSSequence, DBPQualifiedObject
{
    static final Log log = Log.getLog(PostgreSequence.class);

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private Number lastValue;
        private Number minValue;
        private Number maxValue;
        private Number incrementBy;
        public String description;

        @Property(viewable = true, editable = true, updatable = true, order = 1)
        public Number getLastValue() {
            return lastValue;
        }
        @Property(viewable = true, editable = true, updatable = true, order = 2)
        public Number getMinValue() {
            return minValue;
        }
        @Property(viewable = true, editable = true, updatable = true, order = 3)
        public Number getMaxValue() {
            return maxValue;
        }
        @Property(viewable = true, editable = true, updatable = true, order = 4)
        public Number getIncrementBy() {
            return incrementBy;
        }
        @Property(viewable = true, editable = true, updatable = true, order = 10)
        public String getDescription() {
            return description;
        }
    }
    public static class AdditionalInfoValidator implements IPropertyCacheValidator<PostgreSequence> {
        @Override
        public boolean isPropertyCached(PostgreSequence object, Object propertyId)
        {
            return object.additionalInfo.loaded;
        }
    }

    private PostgreSchema schema;
    private int oid;
    private String name;
    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public PostgreSequence(PostgreSchema schema, JDBCResultSet dbResult) {
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

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return schema.getDatabase();
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
    public String getDescription() {
        return additionalInfo.description;
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

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load sequence additional info")) {
            try (JDBCPreparedStatement dbSeqStat = session.prepareStatement(
                "SELECT last_value,min_value,max_value,increment_by from " + DBUtils.getQuotedIdentifier(schema) + "." + DBUtils.getQuotedIdentifier(this))) {
                try (JDBCResultSet seqResults = dbSeqStat.executeQuery()) {
                    if (seqResults.next()) {
                        additionalInfo.lastValue = JDBCUtils.safeGetLong(seqResults, 1);
                        additionalInfo.minValue = JDBCUtils.safeGetLong(seqResults, 2);
                        additionalInfo.maxValue = JDBCUtils.safeGetLong(seqResults, 3);
                        additionalInfo.incrementBy = JDBCUtils.safeGetLong(seqResults, 4);
                    }
                }
            }
            try {
                additionalInfo.description = PostgreUtils.getObjectComment(session.getProgressMonitor(), getDataSource(), schema.getName(), name);
            } catch (Exception e) {
                log.warn("Error reading sequence description", e);
            }
            additionalInfo.loaded = true;
        } catch (Exception e) {
            log.warn("Error reading sequence values", e);
        }
    }

    @Override
    public Number getLastValue() {
        return additionalInfo.lastValue;
    }

    @Override
    public Number getMinValue() {
        return additionalInfo.minValue;
    }

    @Override
    public Number getMaxValue() {
        return additionalInfo.maxValue;
    }

    @Override
    public Number getIncrementBy() {
        return additionalInfo.incrementBy;
    }

    ///////////////////////////////////////////////////////////////////////
    // Entity

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return false;
    }

    @Override
    public DBSEntityType getEntityType() {
        return PostgreConstants.ENTITY_TYPE_SEQUENCE;
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
