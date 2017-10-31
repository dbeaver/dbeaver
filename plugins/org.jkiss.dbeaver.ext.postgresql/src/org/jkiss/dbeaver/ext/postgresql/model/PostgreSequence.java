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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
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
import java.util.Map;

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

        @Property(viewable = true, editable = true, updatable = true, order = 10)
        public Number getLastValue() {
            return lastValue;
        }
        @Property(viewable = true, editable = true, updatable = true, order = 11)
        public Number getMinValue() {
            return minValue;
        }
        @Property(viewable = true, editable = true, updatable = true, order = 12)
        public Number getMaxValue() {
            return maxValue;
        }
        @Property(viewable = true, editable = true, updatable = true, order = 13)
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
                "SELECT last_value,min_value,max_value,increment_by from " + getFullyQualifiedName(DBPEvaluationContext.DML))) {
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
            sql.append("\nINCREMENT BY ").append(info.getIncrementBy());
        }
        if (info.getMinValue() != null && info.getMinValue().longValue() > 0) {
            sql.append("\nMINVALUE ").append(info.getMinValue());
        } else {
            sql.append("\nNO MINVALUE");
        }
        if (info.getMaxValue() != null && info.getMaxValue().longValue() > 0) {
            sql.append("\nMAXVALUE ").append(info.getMaxValue());
        } else {
            sql.append("\nNO MAXVALUE");
        }
        if (info.getLastValue() != null && info.getLastValue().longValue() > 0) {
            sql.append("\nSTART ").append(info.getLastValue());
        }


        return sql.toString();
    }

}
