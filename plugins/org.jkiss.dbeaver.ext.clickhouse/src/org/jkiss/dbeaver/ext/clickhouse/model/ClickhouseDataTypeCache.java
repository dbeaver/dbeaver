/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeArray;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeCache;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class ClickhouseDataTypeCache extends GenericDataTypeCache {

    public ClickhouseDataTypeCache(GenericStructContainer container) {
        super(container);
    }

    @Override
    protected void addCustomObjects(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer owner, @NotNull List<GenericDataType> genericDataTypes) {
        if (DBUtils.findObject(genericDataTypes, "Int128") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.NUMERIC, "Int128", "Int128", false, false, 0, 0, 0));
        }
        if (DBUtils.findObject(genericDataTypes, "Int256") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.NUMERIC, "Int256", "Int256", false, false, 0, 0, 0));
        }
        if (DBUtils.findObject(genericDataTypes, "UInt128") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.NUMERIC, "UInt128", "UInt128", false, false, 0, 0, 0));
        }
        if (DBUtils.findObject(genericDataTypes, "UInt256") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.NUMERIC, "UInt256", "UInt256", false, false, 0, 0, 0));
        }
        if (DBUtils.findObject(genericDataTypes, "Decimal") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.DECIMAL, "Decimal", "Decimal", false, false, 0, 0, 0));
        }
        if (DBUtils.findObject(genericDataTypes, "Bool") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.BOOLEAN, "Bool", "Bool", false, false, 0, 0, 0));
        }
        // Add array data types
        for (GenericDataType dt : new ArrayList<>(genericDataTypes)) {
            genericDataTypes.add(new GenericDataTypeArray(dt.getParentObject(), Types.ARRAY, "Array(" + dt.getName() + ")", "Array of " + dt.getName(), dt));
        }
        // Driver error - missing data types
        if (DBUtils.findObject(genericDataTypes, "DateTime64") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.TIMESTAMP, "DateTime64", "DateTime64", false, false, 0, 0, 0));
        }
    }


    /**
     * The query the clickhouse-0.8.5 driver sends to the database is written with syntax unsupported for old server versions.
     * We took the query and rewrote it without using this new ClickHouse syntax.
     * Also, we added data kinds for data types - attrs.c6, otherwise we can't know the data kind by the type name
     */
    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer genericStructContainer
    ) throws SQLException {
        record Entry(
            String name,
            int precision,
            boolean isUnsigned,
            int minimumScale,
            int maximumScale,
            int sqlDataType
        ) {
            public String toSelectStatement(boolean withColumnNames) {
                List<Object> values = List.of(
                    "'" + name + "'",
                    precision,
                    isUnsigned,
                    minimumScale,
                    maximumScale,
                    sqlDataType
                );
                return "select " + (
                    withColumnNames
                        ? IntStream.range(0, values.size())
                            .mapToObj(i -> values.get(i).toString() + " as c" + (i + 1))
                            .collect(Collectors.joining(","))
                        : values.stream().map(Object::toString).collect(Collectors.joining(","))
                    );
            }
        }
        
        List<Entry> knownTypeEntries = List.of(
            new Entry("Bool", 1, true, 0, 0, Types.BOOLEAN),
            new Entry("Date", 10, false, 0, 0, Types.DATE),
            new Entry("Date32", 10, false, 0, 0, Types.DATE),
            new Entry("DateTime", 29, false, 0, 9, Types.TIMESTAMP),
            new Entry("DateTime32", 19, false, 0, 0, Types.TIMESTAMP),
            new Entry("DateTime64", 29, false, 0, 9, Types.TIMESTAMP),
            new Entry("Enum", 0, false, 0, 0, 0),
            new Entry("Enum8", 0, false, 0, 0, 0),
            new Entry("Enum16", 0, false, 0, 0, 0),
            new Entry("FixedString", 0, false, 0, 0, Types.VARCHAR),
            new Entry("Int8", 3, true, 0, 0, Types.NUMERIC),
            new Entry("UInt8", 3, false, 0, 0, Types.NUMERIC),
            new Entry("Int16", 5, true, 0, 0, Types.NUMERIC),
            new Entry("UInt16", 5, false, 0, 0, Types.NUMERIC),
            new Entry("Int32", 10, true, 0, 0, Types.NUMERIC),
            new Entry("UInt32", 10, false, 0, 0, Types.NUMERIC),
            new Entry("Int64", 19, true, 0, 0, Types.NUMERIC),
            new Entry("IntervalYear", 19, true, 0, 0, 0),
            new Entry("IntervalQuarter", 19, true, 0, 0, 0),
            new Entry("IntervalMonth", 19, true, 0, 0, 0),
            new Entry("IntervalWeek", 19, true, 0, 0, 0),
            new Entry("IntervalDay", 19, true, 0, 0, 0),
            new Entry("IntervalHour", 19, true, 0, 0, 0),
            new Entry("IntervalMinute", 19, true, 0, 0, 0),
            new Entry("IntervalSecond", 19, true, 0, 0, 0),
            new Entry("IntervalMicrosecond", 19, true, 0, 0, 0),
            new Entry("IntervalMillisecond", 19, true, 0, 0, 0),
            new Entry("IntervalNanosecond", 19, true, 0, 0, 0),
            new Entry("UInt64", 20, false, 0, 0, Types.NUMERIC),
            new Entry("Int128", 39, true, 0, 0, Types.NUMERIC),
            new Entry("UInt128", 39, false, 0, 0, Types.NUMERIC),
            new Entry("Int256", 77, true, 0, 0, Types.NUMERIC),
            new Entry("UInt256", 78, false, 0, 0, Types.NUMERIC),
            new Entry("Decimal", 76, true, 0, 76, Types.NUMERIC),
            new Entry("Decimal32", 9, true, 0, 9, Types.NUMERIC),
            new Entry("Decimal64", 18, true, 0, 18, Types.NUMERIC),
            new Entry("Decimal128", 38, true, 0, 38, Types.NUMERIC),
            new Entry("Decimal256", 76, true, 0, 76, Types.NUMERIC),
            new Entry("BFloat16", 3, true, 0, 16, Types.NUMERIC),
            new Entry("Float32", 12, true, 0, 38, Types.NUMERIC),
            new Entry("Float64", 22, true, 0, 308, Types.NUMERIC),
            new Entry("IPv4", 10, false, 0, 0, 0),
            new Entry("IPv6", 39, false, 0, 0, 0),
            new Entry("UUID", 69, false, 0, 0, 0),
            new Entry("Point", 0, true, 0, 0, 0),
            new Entry("Polygon", 0, true, 0, 0, 0),
            new Entry("MultiPolygon", 0, true, 0, 0, 0),
            new Entry("Ring", 0, true, 0, 0, 0),
            new Entry("LineString", 0, true, 0, 0, 0),
            new Entry("MultiLineString", 0, true, 0, 0, 0),
            new Entry("JSON", 0, false, 0, 0, 0),
            new Entry("Object", 0, false, 0, 0, 0),
            new Entry("String", 0, false, 0, 0, Types.VARCHAR),
            new Entry("Array", 0, false, 0, 0, Types.ARRAY),
            new Entry("Map", 0, false, 0, 0, 0),
            new Entry("Nested", 0, false, 0, 0, 0),
            new Entry("Tuple", 0, false, 0, 0, 0),
            new Entry("Nothing", 0, false, 0, 0, 0),
            new Entry("LowCardinality", 0, false, 0, 0, 0),
            new Entry("Nullable", 0, false, 0, 0, 0),
            new Entry("SimpleAggregateFunction", 0, false, 0, 0, 0),
            new Entry("AggregateFunction", 0, false, 0, 0, 0),
            new Entry("Variant", 0, false, 0, 0, 0),
            new Entry("Dynamic", 0, false, 0, 0, 0)
        );

        String sql = """
            SELECT
                dt.name AS TYPE_NAME,
                dt.alias_to AS TYPE_ALIAS, -- in driver, it was if(empty(alias_to), name, alias_to) AS DATA_TYPE
                attrs.c2 AS PRECISION,
                NULL AS LITERAL_PREFIX,
                NULL AS LITERAL_SUFFIX,
                NULL AS CREATE_PARAMS,
                dt.name AS NULLABLE,
                not(dt.case_insensitive)::Boolean AS CASE_SENSITIVE,
                3 AS SEARCHABLE,
                not(attrs.c3)::Boolean AS UNSIGNED_ATTRIBUTE,
                false AS FIXED_PREC_SCALE,
                false AS AUTO_INCREMENT,
                dt.name AS LOCAL_TYPE_NAME,
                attrs.c4 AS MINIMUM_SCALE,
                attrs.c5 AS MAXIMUM_SCALE,
                attrs.c6 AS DATA_TYPE, -- it's our attribute with data kind information
                0 AS SQL_DATETIME_SUB,
                0 AS NUM_PREC_RADIX
            FROM system.data_type_families dt
            LEFT JOIN (""" +
            knownTypeEntries.get(0).toSelectStatement(true) + " UNION ALL " +
            knownTypeEntries.stream().skip(1).map(e -> e.toSelectStatement(false)).collect(Collectors.joining(" UNION ALL ")) +
            """
            ) as attrs ON (dt.name = attrs.c1 or dt.alias_to = attrs.c1)
            """;
        // Also WHERE dt.alias_to = '' was in the driver's query, but we decided we want aliased types with aliases
        return session.prepareStatement(sql);
    }
}
