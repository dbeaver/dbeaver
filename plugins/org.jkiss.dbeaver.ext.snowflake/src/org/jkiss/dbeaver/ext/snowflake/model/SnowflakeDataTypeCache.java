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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeCache;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;

import java.sql.Types;
import java.util.List;

public class SnowflakeDataTypeCache extends GenericDataTypeCache {

    SnowflakeDataTypeCache(GenericStructContainer owner) {
        super(owner);
    }

    @Override
    protected void addCustomObjects(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer owner, @NotNull List<GenericDataType> genericDataTypes) {
        if (DBUtils.findObject(genericDataTypes, SQLConstants.DATA_TYPE_BIGINT) == null) {
            genericDataTypes.add(new GenericDataType(
                this.owner,
                Types.BIGINT,
                SQLConstants.DATA_TYPE_BIGINT,
                SQLConstants.DATA_TYPE_BIGINT,
                false,
                false,
                0,
                0,
                0));
        }
        if (DBUtils.findObject(genericDataTypes, SQLConstants.DATA_TYPE_INT) == null) {
            genericDataTypes.add(new GenericDataType(
                this.owner,
                Types.INTEGER,
                SQLConstants.DATA_TYPE_INT,
                SQLConstants.DATA_TYPE_INT,
                false,
                false,
                0,
                0,
                0));
        }
        if (DBUtils.findObject(genericDataTypes, SnowflakeConstants.TYPE_DOUBLE_PRECISION) == null) {
            genericDataTypes.add(new GenericDataType(
                this.owner,
                Types.DOUBLE,
                SnowflakeConstants.TYPE_DOUBLE_PRECISION,
                SnowflakeConstants.TYPE_DOUBLE_PRECISION,
                false,
                false,
                0,
                0,
                0));
        }
        if (DBUtils.findObject(genericDataTypes, SnowflakeConstants.TYPE_REAL) == null) {
            genericDataTypes.add(new GenericDataType(
                this.owner,
                Types.REAL,
                SnowflakeConstants.TYPE_REAL,
                SnowflakeConstants.TYPE_REAL,
                false,
                false,
                0,
                0,
                0));
        }
        if (DBUtils.findObject(genericDataTypes, SQLConstants.DATA_TYPE_FLOAT) == null) {
            genericDataTypes.add(new GenericDataType(
                this.owner,
                Types.FLOAT,
                SQLConstants.DATA_TYPE_FLOAT,
                SQLConstants.DATA_TYPE_FLOAT,
                false,
                false,
                0,
                0,
                0));
        }
        if (DBUtils.findObject(genericDataTypes, SnowflakeConstants.TYPE_DECIMAL) == null) {
            genericDataTypes.add(new GenericDataType(
                this.owner,
                Types.DECIMAL,
                SnowflakeConstants.TYPE_DECIMAL,
                SnowflakeConstants.TYPE_DECIMAL,
                false,
                false,
                SnowflakeConstants.NUMERIC_MAX_PRECISION,
                0,
                SnowflakeConstants.NUMERIC_MAX_PRECISION - 1));
        }
    }
}
