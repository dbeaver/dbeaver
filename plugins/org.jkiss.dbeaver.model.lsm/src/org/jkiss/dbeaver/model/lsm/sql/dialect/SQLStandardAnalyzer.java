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
package org.jkiss.dbeaver.model.lsm.sql.dialect;


import org.antlr.v4.runtime.CommonTokenStream;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerImpl;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.stm.STMSource;
import org.jkiss.dbeaver.model.stm.STMTreeRuleNode;
import org.jkiss.utils.Pair;

public class SQLStandardAnalyzer extends LSMAnalyzerImpl<SQLStandardLexer, SQLStandardParser> {
    @NotNull
    @Override
    protected Pair<SQLStandardLexer, SQLStandardParser> createParser(@NotNull STMSource source) {
        SQLStandardLexer lexer = new SQLStandardLexer(source.getStream());
        SQLStandardParser parser =  new SQLStandardParser(new CommonTokenStream(lexer));
        return new Pair<>(lexer, parser);
    }

    @NotNull
    @Override
    protected STMTreeRuleNode parseSqlQueryImpl(@NotNull SQLStandardParser parser) {
        return parser.sqlQuery();
    }
}
