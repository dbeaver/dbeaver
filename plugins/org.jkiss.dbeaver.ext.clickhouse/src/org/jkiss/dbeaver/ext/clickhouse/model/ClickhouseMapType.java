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

public class ClickhouseMapType extends ClickhouseTupleType {
    public ClickhouseMapType(@NotNull ClickhouseDataSource dataSource, @NotNull DBSDataType keyType, @NotNull DBSDataType valueType) {
        super(dataSource, new DBSDataType[]{keyType, valueType}, new String[]{"Key", "Value"});
    }
}
