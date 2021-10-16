/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
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
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * PostgreSequence
 */
public class PostgreSequence extends PostgreTableBase implements DBSSequence, DBPQualifiedObject
{
    private static final Log log = Log.getLog(PostgreSequence.class);

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private long startValue;
        private Long lastValue;
        private long minValue;
        private long maxValue;
        private long incrementBy;
        private long cacheValue;
        private boolean isCycled;

        @Property(viewable = true, editable = true, updatable = true, order = 10)
        public Long getLastValue() {
            return lastValue;
        }

        public void setLastValue(long lastValue) {
            this.lastValue = lastValue;
        }

        @Property(viewable = true, editable = true, updatable = true, order = 20)
        public long getStartValue() {
            return startValue;
        }

        public void setStartValue(long startValue) {
            this.startValue = startValue;
        }

        @Property(viewable = true, editable = true, updatable = true, order = 21)
        public long getMinValue() {
            return minValue;
        }

        public void setMinValue(long minValue) {
            this.minValue = minValue;
        }

        @Property(viewable = true, editable = true, updatable = true, order = 22)
        public long getMaxValue() {
            return maxValue;
        }

        public void setMaxValue(long maxValue) {
            this.maxValue = maxValue;
        }

        @Property(viewable = true, editable = true, updatable = true, order = 23)
        public long getIncrementBy() {
            return incrementBy;
        }

        public void setIncrementBy(long incrementBy) {
            this.incrementBy = incrementBy;
        }

        @Property(viewable = true, editable = true, updatable = true, order = 24)
        public long getCacheValue() {
            return cacheValue;
        }

        public void setCacheValue(long cacheValue) {
            this.cacheValue = cacheValue;
        }

        @Property(viewable = true, editable = true, updatable = true, order = 25)
        public boolean isCycled() {
            return isCycled;
        }

        public void setCycled(boolean cycled) {
            isCycled = cycled;
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

    public PostgreSequence(PostgreSchema catalog) {
        super(catalog);
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
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load sequence additional info")) {
            if (getDataSource().isServerVersionAtLeast(10, 0)) {
                try (JDBCPreparedStatement dbSeqStat = session.prepareStatement(
                    "SELECT * from pg_catalog.pg_sequences WHERE schemaname=? AND sequencename=?")) {
                    dbSeqStat.setString(1, getSchema().getName());
                    dbSeqStat.setString(2, getName());
                    try (JDBCResultSet seqResults = dbSeqStat.executeQuery()) {
                        if (seqResults.next()) {
                            additionalInfo.startValue = JDBCUtils.safeGetLong(seqResults, "start_value");
                            additionalInfo.lastValue = JDBCUtils.safeGetLongNullable(seqResults, "last_value");
                            additionalInfo.minValue = JDBCUtils.safeGetLong(seqResults, "min_value");
                            additionalInfo.maxValue = JDBCUtils.safeGetLong(seqResults, "max_value");
                            additionalInfo.incrementBy = JDBCUtils.safeGetLong(seqResults, "increment_by");
                            additionalInfo.cacheValue = JDBCUtils.safeGetLong(seqResults, "cache_size");
                            additionalInfo.isCycled = JDBCUtils.safeGetBoolean(seqResults, "cycle");
                        }
                    }
                }
            } else {
                try (JDBCPreparedStatement dbSeqStat = session.prepareStatement(
                    "SELECT * from " + getFullyQualifiedName(DBPEvaluationContext.DML))) {
                    try (JDBCResultSet seqResults = dbSeqStat.executeQuery()) {
                        if (seqResults.next()) {
                            additionalInfo.startValue = JDBCUtils.safeGetLong(seqResults, "start_value");
                            additionalInfo.lastValue = JDBCUtils.safeGetLongNullable(seqResults, "last_value");
                            additionalInfo.minValue = JDBCUtils.safeGetLong(seqResults, "min_value");
                            additionalInfo.maxValue = JDBCUtils.safeGetLong(seqResults, "max_value");
                            additionalInfo.incrementBy = JDBCUtils.safeGetLong(seqResults, "increment_by");
                            additionalInfo.cacheValue = JDBCUtils.safeGetLong(seqResults, "cache_size");
                            additionalInfo.isCycled = JDBCUtils.safeGetBoolean(seqResults, "is_cycled");
                        }
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

    @Override
    public String getTableTypeName() {
        return "SEQUENCE";
    }

    ///////////////////////////////////////////////////////////////////////
    // Entity

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return DBSEntityType.SEQUENCE;
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

    @Nullable
    @Override
    public String[] getRelOptions() {
        return null;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        AdditionalInfo info = getAdditionalInfo(monitor);
        StringBuilder sql = new StringBuilder()
            .append("-- DROP SEQUENCE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(";\n\n")
            .append("CREATE SEQUENCE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL));

        if (info.getIncrementBy() > 0) {
            sql.append("\n\tINCREMENT BY ").append(info.getIncrementBy());
        }
        if (info.getMinValue() > 0) {
            sql.append("\n\tMINVALUE ").append(info.getMinValue());
        } else {
            sql.append("\n\tNO MINVALUE");
        }
        if (info.getMaxValue() > 0) {
            sql.append("\n\tMAXVALUE ").append(info.getMaxValue());
        } else {
            sql.append("\n\tNO MAXVALUE");
        }
        if (info.getStartValue() > 0) {
            sql.append("\n\tSTART ").append(info.getStartValue());
        }
        if (info.getCacheValue() > 0) {
            sql.append("\n\tCACHE ").append(info.getCacheValue());
            sql.append("\n\t").append(info.isCycled ? "" : "NO ").append("CYCLE");
        }
        sql.append(';');

		if (!CommonUtils.isEmpty(getDescription())) {
			sql.append("\nCOMMENT ON SEQUENCE ").append(DBUtils.getQuotedIdentifier(this)).append(" IS ")
					.append(SQLUtils.quoteString(this, getDescription())).append(";");
		}
        
        List<DBEPersistAction> actions = new ArrayList<>();
        PostgreUtils.getObjectGrantPermissionActions(monitor, this, actions, options);
        if (!actions.isEmpty()) {
            sql.append("\n\n");
            sql.append(SQLUtils.generateScript(getDataSource(), actions.toArray(new DBEPersistAction[actions.size()]), false));
        }

        return sql.toString();
    }

    public String generateChangeOwnerQuery(String owner) {
        return "ALTER SEQUENCE " + DBUtils.getObjectFullName(this, DBPEvaluationContext.DDL) + " OWNER TO " + owner;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        additionalInfo.loaded = false;
        return super.refreshObject(monitor);
    }
}
