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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
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
        private Number startValue;
        private Number lastValue;
        private Number minValue;
        private Number maxValue;
        private Number incrementBy;
        private Number cacheValue;
        private boolean isCycled;

        @Property(viewable = true, editable = true, updatable = false, order = 10)
        public Number getLastValue() {
            return lastValue;
        }
        @Property(viewable = true, editable = true, updatable = false, order = 20)
        public Number getStartValue() {
            return startValue;
        }
        @Property(viewable = true, editable = true, updatable = false, order = 21)
        public Number getMinValue() {
            return minValue;
        }
        @Property(viewable = true, editable = true, updatable = false, order = 22)
        public Number getMaxValue() {
            return maxValue;
        }
        @Property(viewable = true, editable = true, updatable = false, order = 23)
        public Number getIncrementBy() {
            return incrementBy;
        }
        @Property(viewable = true, editable = true, updatable = false, order = 24)
        public Number getCacheValue() {
            return cacheValue;
        }
        @Property(viewable = true, editable = true, updatable = false, order = 25)
        public boolean isCycled() {
            return isCycled;
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
                            additionalInfo.lastValue = JDBCUtils.safeGetLong(seqResults, "last_value");
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
                            additionalInfo.lastValue = JDBCUtils.safeGetLong(seqResults, "last_value");
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

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        AdditionalInfo info = getAdditionalInfo(monitor);
        StringBuilder sql = new StringBuilder();
        sql.append("-- DROP SEQUENCE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(";\n\n");
        sql.append("CREATE SEQUENCE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL));
        if (info.getIncrementBy() != null && info.getIncrementBy().longValue() > 0) {
            sql.append("\n\tINCREMENT BY ").append(info.getIncrementBy());
        }
        if (info.getMinValue() != null && info.getMinValue().longValue() > 0) {
            sql.append("\n\tMINVALUE ").append(info.getMinValue());
        } else {
            sql.append("\n\tNO MINVALUE");
        }
        if (info.getMaxValue() != null && info.getMaxValue().longValue() > 0) {
            sql.append("\n\tMAXVALUE ").append(info.getMaxValue());
        } else {
            sql.append("\n\tNO MAXVALUE");
        }
        Number startValue = info.getStartValue();
        if (startValue != null && startValue.longValue() > 0) {
            sql.append("\n\tSTART ").append(startValue);
        }
        if (info.getCacheValue() != null && info.getCacheValue().longValue() > 0) {
            sql.append("\n\tCACHE ").append(info.getCacheValue())
                .append("\n\t").append(info.isCycled ? "" : "NO ").append("CYCLE")
                .append(";");
        }

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
