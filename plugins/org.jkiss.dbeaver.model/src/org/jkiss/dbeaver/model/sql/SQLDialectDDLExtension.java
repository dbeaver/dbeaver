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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Sql dialect ddl extension.
 */
public interface SQLDialectDDLExtension extends SQLDialect {

    /**
     * Gets auto increment keyword.
     *
     * @return the auto increment keyword
     */
    @Nullable
    String getAutoIncrementKeyword();

    /**
     * Supports create if exists boolean.
     *
     * @return true or false
     */
    boolean supportsCreateIfExists();

    /**
     * Gets timestamp type.
     *
     * @return the timestamp type
     */
    @NotNull
    String getTimestampDataType();

    /**
     * Gets big integer type.
     *
     * @return the big integer type
     */
    @NotNull
    String getBigIntegerType();

    /**
     * Gets clob data type.
     *
     * @return the clob data type
     */
    @NotNull
    String getClobDataType();

    /**
     * Gets blob data type.
     *
     * @return the blob data type
     */
    @NotNull
    String getBlobDataType();

    /**
     * Gets uuid data type.
     *
     * @return the uuid data type
     */
    @NotNull
    String getUuidDataType();

    /**
     * Gets boolean data type.
     *
     * @return the boolean data type
     */
    @NotNull
    String getBooleanDataType();

    /**
     * Gets alter column operation.
     */
    @NotNull
    String getAlterColumnOperation();

    boolean supportsNoActionIndex();

    /**
     * Checks if sql dialect supports SET key word for alter column.
     */
    boolean supportsAlterColumnSet();

    /**
     * Checks if sql dialect supports COLUMN key word for alter column.
     */
    boolean supportsAlterHasColumn();
}
