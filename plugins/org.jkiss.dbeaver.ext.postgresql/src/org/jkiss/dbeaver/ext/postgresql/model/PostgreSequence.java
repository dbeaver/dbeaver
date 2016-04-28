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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
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
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.util.Collection;

/**
 * PostgreSequence
 */
public class PostgreSequence extends PostgreTableBase implements DBSSequence, DBPQualifiedObject
{
    private static final Log log = Log.getLog(PostgreSequence.class);

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private Number lastValue;
        private Number minValue;
        private Number maxValue;
        private Number incrementBy;

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
    }
    public static class AdditionalInfoValidator implements IPropertyCacheValidator<PostgreSequence> {
        @Override
        public boolean isPropertyCached(PostgreSequence object, Object propertyId)
        {
            return object.additionalInfo.loaded;
        }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public PostgreSequence(PostgreSchema schema, JDBCResultSet dbResult) {
        super(schema, dbResult);
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
                "SELECT last_value,min_value,max_value,increment_by from " + getFullQualifiedName())) {
                try (JDBCResultSet seqResults = dbSeqStat.executeQuery()) {
                    if (seqResults.next()) {
                        additionalInfo.lastValue = JDBCUtils.safeGetLong(seqResults, 1);
                        additionalInfo.minValue = JDBCUtils.safeGetLong(seqResults, 2);
                        additionalInfo.maxValue = JDBCUtils.safeGetLong(seqResults, 3);
                        additionalInfo.incrementBy = JDBCUtils.safeGetLong(seqResults, 4);
                    }
                }
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

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return PostgreConstants.ENTITY_TYPE_SEQUENCE;
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {

    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

}
