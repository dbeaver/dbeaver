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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;

public class SQLServerDialectMssql extends SQLServerDialectBase {

    public SQLServerDialectMssql() {
        super("SQLServer", "sqlserver");
    }

    @NotNull
    @Override
    public ProceduralSyntaxFamily getProceduralSyntaxFamily() {
        return ProceduralSyntaxFamily.TSQL;
    }

    /** T-SQL variables declared inside a procedure remain visible across nested BEGIN/END blocks.
     *
     * @see <a href="https://learn.microsoft.com/en-us/sql/t-sql/language-elements/declare-local-variable-transact-sql">DECLARE</a>
     */
    @NotNull
    @Override
    public LocalVariableVisibility getLocalVariableVisibility(@NotNull LocalVariableKind kind) {
        return LocalVariableVisibility.PROGRAM;
    }

}
