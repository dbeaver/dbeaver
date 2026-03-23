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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamoDBSchemaInference {

    @NotNull
    public static DynamoDBResultSetMetaData inferMetaData(
            @NotNull List<Map<String, AttributeValue>> items) {
        Map<String, String> columnTypes = new LinkedHashMap<>();

        for (Map<String, AttributeValue> item : items) {
            for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                String name = entry.getKey();
                String type = DynamoDBTypeMapper.inferDynamoType(entry.getValue());

                if (!columnTypes.containsKey(name)) {
                    columnTypes.put(name, type);
                } else {
                    String existingType = columnTypes.get(name);
                    if (!existingType.equals(type)) {
                        columnTypes.put(name, "S"); // Promote to string on conflict
                    }
                }
            }
        }

        List<DynamoDBResultSetColumn> columns = new ArrayList<>();
        int ordinal = 0;
        for (Map.Entry<String, String> entry : columnTypes.entrySet()) {
            columns.add(new DynamoDBResultSetColumn(entry.getKey(), entry.getValue(), ordinal++));
        }
        return new DynamoDBResultSetMetaData(columns);
    }
}
