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

package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.local.LocalResultSetColumn;
import org.jkiss.dbeaver.model.impl.local.LocalResultSetMeta;
import org.jkiss.utils.CommonUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stream producer result set
 */
public class StreamTransferResultSet implements DBCResultSet {

    private static final Log log = Log.getLog(StreamTransferResultSet.class);

    private final DBCSession session;
    private final DBCStatement statement;
    private final StreamEntityMapping entityMapping;
    private final List<DBCAttributeMetaData> metaAttrs;
    // Stream row: values in source attributes order
    private Object[] streamRow;
    private final List<StreamDataImporterColumnInfo> attributeMappings;
    private DateTimeFormatter dateTimeFormat;
    private ZoneId dateTimeZoneId;

    public StreamTransferResultSet(DBCSession session, DBCStatement statement, StreamEntityMapping entityMapping) {
        this.session = session;
        this.statement = statement;
        this.entityMapping = entityMapping;
        this.attributeMappings = this.entityMapping.getStreamColumns();
        this.metaAttrs = attributeMappings.stream()
            .map(c -> new LocalResultSetColumn(this, c.getOrdinalPosition(), c.getName(), c))
            .collect(Collectors.toList());
    }

    public List<StreamDataImporterColumnInfo> getAttributeMappings() {
        return attributeMappings;
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
        StreamDataImporterColumnInfo attr = this.attributeMappings.get(index);

        Object value = streamRow[index];
        if (value != null && dateTimeFormat != null && attr.getDataKind() == DBPDataKind.DATETIME && !(value instanceof Date)) {
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
                    LocalDateTime localDT;
                    if (ta.isSupported(ChronoField.NANO_OF_SECOND)) {
                        localDT = LocalDateTime.from(ta);
                    } else {
                        localDT = LocalDate.from(ta).atStartOfDay();
                        log.debug("No time present in datetime string, defaulting to the start of the day");
                    }
                    if (dateTimeZoneId != null) {
                        // Shift LocalDateTime to specified zone
                        // https://stackoverflow.com/questions/42280454/changing-localdatetime-based-on-time-difference-in-current-time-zone-vs-eastern
                        localDT = localDT
                            .atZone(ZoneId.systemDefault())
                            .withZoneSameInstant(dateTimeZoneId)
                            .toLocalDateTime();
                    }
                    // We use java.sql.Timestamp.valueOf because classic date/time conversion turns "pre-historic" Gregorian
                    // dates into incorrect SQL timestamps (in Julian calendar). E.g. 0001-01-01->0001-01-03
                    value = Timestamp.valueOf(localDT);
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

    public void setDateTimeFormat(DateTimeFormatter dateTimeFormat, ZoneId dateTimeZoneId) {
        this.dateTimeFormat = dateTimeFormat;
        this.dateTimeZoneId = dateTimeZoneId;
        if (this.dateTimeFormat != null && this.dateTimeZoneId != null) {
            // Set zone to the format.
            // FIXME: it looks like a good idea but in fact iti s not. We can't convert ZonedDateTime into
            // FIXME: proper SQL timestamp for pre-historic (pre-Gregorian) dates.
            // FIXME: so we will shift LocalDateTime in getAttributeValue instead
            // FIXME: https://stackoverflow.com/questions/23975205/why-does-converting-java-dates-before-1582-to-localdate-with-instant-give-a-diff
            //this.dateTimeFormat = this.dateTimeFormat.withZone(dateTimeZoneId);
        }
    }
}
