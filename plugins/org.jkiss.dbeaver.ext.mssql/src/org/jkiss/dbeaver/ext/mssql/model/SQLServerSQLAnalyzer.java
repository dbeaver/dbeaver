/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.dialect.SQLStandardAnalyzer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.stm.STMErrorListener;
import org.jkiss.dbeaver.model.stm.STMSource;

public class SQLServerSQLAnalyzer extends SQLStandardAnalyzer {
    @NotNull
    @Override
    protected SQLStandardParser prepareParser(@NotNull STMSource source, @Nullable STMErrorListener errorListener) {
        SQLStandardParser parser = super.prepareParser(source, errorListener);
        parser.setIsSupportSquareBracketQuotation(true);
        return parser;
    }
}