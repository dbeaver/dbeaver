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

package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.local.LocalResultSetColumn;
import org.jkiss.dbeaver.model.impl.local.LocalResultSetMeta;
import org.jkiss.utils.CommonUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

/**
 * Stream producer result set
 */
public class StreamTransferResultSet implements DBCResultSet {

    private static final Log log = Log.getLog(StreamTransferResultSet.class);

    private final DBCSession session;
    private final DBCStatement statement;
    private StreamProducerSettings.EntityMapping entityMapping;
    private List<DBCAttributeMetaData> metaAttrs;
    // Stream row: values in source attributes order
    private Object[] streamRow;
    private final List<StreamProducerSettings.AttributeMapping> attributeMappings;
    // Maps target attributes indexes to source attributes indexes
    // (not indexes in source data, it is controlled by AttributeMapping.sourceAttributeIndex)
    private final int[] targetToSourceMap;
    private DateTimeFormatter dateTimeFormat;

    public StreamTransferResultSet(DBCSession session, DBCStatement statement, StreamProducerSettings.EntityMapping entityMapping) {
        this.session = session;
        this.statement = statement;
        this.entityMapping = entityMapping;
        this.attributeMappings = this.entityMapping.getAttributeMappings();
        this.metaAttrs = new ArrayList<>(attributeMappings.size());
        this.targetToSourceMap = new int[this.entityMapping.getValuableAttributeMappings().size()];
        int mapIndex = 0;
        for (int i = 0; i < attributeMappings.size(); i++) {
            StreamProducerSettings.AttributeMapping attr = attributeMappings.get(i);
            if (attr.isValuable()) {
                metaAttrs.add(new LocalResultSetColumn(this, i, attr.getTargetAttributeName(), DBPDataKind.STRING));
                this.targetToSourceMap[mapIndex++] = i;
            }
        }
    }

    public void setStreamRow(Object[] streamRow) {
        this.streamRow = streamRow;
    }

    @Override
    public DBCSession getSession() {
        return session;
    }

    @Override
    public DBCStatement getSourceStatement() {
        return statement;
    }

    @Override
    public Object getAttributeValue(int index) throws DBCException {
        int sourceIndex = this.targetToSourceMap[index];
        StreamProducerSettings.AttributeMapping attr = this.attributeMappings.get(sourceIndex);

        if (attr.getMappingType() == StreamProducerSettings.AttributeMapping.MappingType.DEFAULT_VALUE) {
            return attr.getDefaultValue();
        }

        Object value = streamRow[attr.getSourceAttributeIndex()];
        if (value != null && dateTimeFormat != null && attr.getTargetAttribute() != null && attr.getTargetAttribute().getDataKind() == DBPDataKind.DATETIME) {
            // Convert string to timestamp
            try {
                String strValue = CommonUtils.toString(value);
                if (CommonUtils.isEmptyTrimmed(strValue)) {
                    return null;
                }
                TemporalAccessor ta = dateTimeFormat.parse(strValue);
                try {
                    ZonedDateTime zdt = ZonedDateTime.from(ta);
                    value = java.util.Date.from(zdt.toInstant());
                } catch (Exception e) {
                    LocalDateTime localDT = LocalDateTime.from(ta);
                    if (localDT != null) {
                        value = java.util.Date.from(localDT.atZone(ZoneId.systemDefault()).toInstant());
                    }
                }
            } catch (Exception e) {
                // Can't parse. Ignore format then
                log.debug("Error parsing datetime string: " + e.getMessage());
            }
        }

        return value;
    }

    @Override
    public Object getAttributeValue(String name) throws DBCException {
        return null;
    }

    @Override
    public DBDValueMeta getAttributeValueMeta(int index) throws DBCException {
        return null;
    }

    @Override
    public DBDValueMeta getRowMeta() throws DBCException {
        return null;
    }

    @Override
    public boolean nextRow() throws DBCException {
        return false;
    }

    @Override
    public boolean moveTo(int position) throws DBCException {
        return false;
    }

    @NotNull
    @Override
    public DBCResultSetMetaData getMeta() throws DBCException {
        return new LocalResultSetMeta(metaAttrs);
    }

    @Override
    public String getResultSetName() throws DBCException {
        return null;
    }

    @Override
    public Object getFeature(String name) {
        return null;
    }

    @Override
    public void close() {

    }

    public DateTimeFormatter getDateTimeFormat() {
        return dateTimeFormat;
    }

    public void setDateTimeFormat(DateTimeFormatter dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
    }
}
