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
package org.jkiss.dbeaver.model.impl.sql;

import org.jkiss.dbeaver.model.sql.SQLDialect;

/**
 * Relational SQL Dialect
 */
public interface RelationalSQLDialect extends SQLDialect {

    /**
     * Standard SQL (SQL2003)
     */
    boolean isStandardSQL();

    boolean supportsOrderBy();

    boolean supportsGroupBy();

    /**
     * @return true if `select count(*) from (select 1,1) z` fails because of duplicate column name
     */
    boolean isAmbiguousCountBroken();

}
