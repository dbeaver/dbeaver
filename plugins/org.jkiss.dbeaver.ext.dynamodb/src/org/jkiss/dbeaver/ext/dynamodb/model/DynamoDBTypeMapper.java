/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.dynamodb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DynamoDBTypeMapper {

    @NotNull
    public static String getTypeName(@Nullable String dynamoType) {
        if (dynamoType == null) return "ANY";
        return switch (dynamoType) {
            case "S" -> "String";
            case "N" -> "Number";
            case "B" -> "Binary";
            case "BOOL" -> "Boolean";
            case "NULL" -> "Null";
            case "SS" -> "StringSet";
            case "NS" -> "NumberSet";
            case "BS" -> "BinarySet";
            case "L" -> "List";
            case "M" -> "Map";
            default -> "Any";
        };
    }

    public static int getJdbcTypeId(@Nullable String dynamoType) {
        if (dynamoType == null) return Types.OTHER;
        return switch (dynamoType) {
            case "S" -> Types.VARCHAR;
            case "N" -> Types.DECIMAL;
            case "B" -> Types.VARBINARY;
            case "BOOL" -> Types.BOOLEAN;
            case "NULL" -> Types.NULL;
            case "SS", "NS", "BS", "L" -> Types.ARRAY;
            case "M" -> Types.STRUCT;
            default -> Types.OTHER;
        };
    }

    @NotNull
    public static DBPDataKind getDataKind(@Nullable String dynamoType) {
        if (dynamoType == null) return DBPDataKind.UNKNOWN;
        return switch (dynamoType) {
            case "S" -> DBPDataKind.STRING;
            case "N" -> DBPDataKind.NUMERIC;
            case "B" -> DBPDataKind.BINARY;
            case "BOOL" -> DBPDataKind.BOOLEAN;
            case "NULL" -> DBPDataKind.UNKNOWN;
            case "SS", "NS", "BS", "L" -> DBPDataKind.ARRAY;
            case "M" -> DBPDataKind.STRUCT;
            default -> DBPDataKind.UNKNOWN;
        };
    }

    @Nullable
    public static Object toJavaValue(@Nullable AttributeValue av) {
        if (av == null) return null;
        if (Boolean.TRUE.equals(av.nul())) return null;
        if (av.s() != null) return av.s();
        if (av.n() != null) {
            String n = av.n();
            try {
                return Long.parseLong(n);
            } catch (NumberFormatException e1) {
                try {
                    return new BigDecimal(n);
                } catch (NumberFormatException e2) {
                    return n;
                }
            }
        }
        if (av.b() != null) return av.b().asByteArray();
        if (av.bool() != null) return av.bool();
        if (av.hasSs()) return av.ss().toString();
        if (av.hasNs()) return av.ns().toString();
        if (av.hasBs()) return "[binary set]";
        if (av.hasL()) {
            return av.l().stream()
                    .map(DynamoDBTypeMapper::toJavaValue)
                    .collect(Collectors.toList()).toString();
        }
        if (av.hasM()) {
            Map<String, Object> map = new LinkedHashMap<>();
            av.m().forEach((k, v) -> map.put(k, toJavaValue(v)));
            return mapToJsonString(map);
        }
        return av.toString();
    }

    @NotNull
    public static String inferDynamoType(@Nullable AttributeValue av) {
        if (av == null) return "NULL";
        if (av.s() != null) return "S";
        if (av.n() != null) return "N";
        if (av.b() != null) return "B";
        if (av.bool() != null) return "BOOL";
        if (Boolean.TRUE.equals(av.nul())) return "NULL";
        if (av.hasSs()) return "SS";
        if (av.hasNs()) return "NS";
        if (av.hasBs()) return "BS";
        if (av.hasL()) return "L";
        if (av.hasM()) return "M";
        return "S";
    }

    private static String mapToJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append("\"").append(entry.getKey()).append("\": ");
            Object val = entry.getValue();
            if (val instanceof String) {
                sb.append("\"").append(val).append("\"");
            } else {
                sb.append(val);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
