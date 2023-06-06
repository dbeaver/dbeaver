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


import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
import org.jkiss.dbeaver.model.lsm.LSMSource;
import org.jkiss.dbeaver.model.lsm.impl.LSMAnalyzerImpl;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.Sql92Lexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.Sql92Parser;
import org.jkiss.dbeaver.model.stm.TreeRuleNode;
import org.jkiss.utils.Pair;
import org.antlr.v4.runtime.CommonTokenStream;

public class Sql92Dialect {
    
    public static class Sql92Analyzer extends LSMAnalyzerImpl<Sql92Lexer, Sql92Parser> {
        @NotNull
        @Override
        protected Pair<Sql92Lexer, Sql92Parser> createParser(@NotNull LSMSource source) {
            Sql92Lexer lexer = new Sql92Lexer(source.getStream());
            Sql92Parser parser =  new Sql92Parser(new CommonTokenStream(lexer));
            return new Pair<>(lexer, parser);
        }

        @NotNull
        @Override
        protected TreeRuleNode parseSqlQueryImpl(@NotNull Sql92Parser parser) {
            return parser.sqlQuery();
        }
    }
    
    private static final LSMAnalyzer dialect = new Sql92Analyzer();
    
    @NotNull
    public static LSMAnalyzer getAnalyzer() {
        return dialect;
    }
}
