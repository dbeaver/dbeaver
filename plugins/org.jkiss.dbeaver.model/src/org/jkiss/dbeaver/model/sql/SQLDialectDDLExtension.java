/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

/**
 * Sql dialect ddl extension.
 */
public interface SQLDialectDDLExtension {

    /**
     * Gets auto increment keyword.
     *
     * @return the auto increment keyword
     */
    String getAutoIncrementKeyword();

    /**
     * Supports create if exists boolean.
     *
     * @return true or false
     */
    boolean supportsCreateIfExists();

    /**
     * Whether datetime should be used instead of timestamp.
     *
     * @return true or false
     */
    boolean timestampAsDatetime();

    /**
     * Gets large numeric type name.
     *
     * @return the large numeric type name
     */
    String getLargeNumericType();

    /**
     * Gets large character type name.
     *
     * @return the large character type name
     */
    String getLargeCharacterType();

}
