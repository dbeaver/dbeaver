/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.utils.Pair;

import java.util.List;

public class ClickhouseMapType extends ClickhouseTupleType {
    private final DBSDataType keyType;
    private final DBSDataType valueType;

    public ClickhouseMapType(
        @NotNull ClickhouseDataSource dataSource,
        @NotNull DBSDataType keyType,
        @NotNull DBSDataType valueType
    ) {
        super(dataSource, "Map", List.of(new Pair<>("Key", keyType), new Pair<>("Value", valueType)));
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @NotNull
    public DBSDataType getKeyType() {
        return keyType;
    }

    @NotNull
    public DBSDataType getValueType() {
        return valueType;
    }
}
