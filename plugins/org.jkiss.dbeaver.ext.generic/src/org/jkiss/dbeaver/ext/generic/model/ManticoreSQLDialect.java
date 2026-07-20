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
package org.jkiss.dbeaver.ext.generic.model;

/**
 * SQL dialect for Manticore Search.
 * <p>
 * Manticore speaks the MySQL wire protocol but does not support table aliases
 * in {@code FROM}/{@code SELECT}. Connecting via the stock MySQL driver uses
 * {@code MySQLDialect}, which enables aliases and breaks auto-generated
 * "View data" queries. This dialect keeps aliases disabled.
 * </p>
 *
 * @see <a href="https://manual.manticoresearch.com/Connecting_to_the_server">Manticore connection docs</a>
 */
public class ManticoreSQLDialect extends GenericSQLDialect {

    public ManticoreSQLDialect() {
        super("Manticore Search", "manticore");
    }

    @Override
    public boolean supportsAliasInSelect() {
        return false;
    }

    @Override
    public boolean supportsAliasInUpdate() {
        return false;
    }

    @Override
    public boolean supportsAliasInConditions() {
        return false;
    }
}
