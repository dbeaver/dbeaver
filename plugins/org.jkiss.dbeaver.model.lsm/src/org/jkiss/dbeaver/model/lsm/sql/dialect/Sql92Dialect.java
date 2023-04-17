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

import java.util.Map;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jkiss.dbeaver.model.lsm.LSMDialect;
import org.jkiss.dbeaver.model.lsm.LSMParser;
import org.jkiss.dbeaver.model.lsm.LSMSource;
import org.jkiss.dbeaver.model.lsm.impl.LSMAnalysisCaseImpl;
import org.jkiss.dbeaver.model.lsm.impl.LSMDialectImpl;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;
import org.jkiss.dbeaver.model.lsm.sql.LSMSelectStatement;
import org.jkiss.dbeaver.model.lsm.sql.impl.SelectStatement;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.Sql92Lexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.Sql92Parser;

public class Sql92Dialect {
    private static final LSMDialect dialect = new LSMDialectImpl(
        Map.of(LSMSelectStatement.class, new LSMAnalysisCaseImpl<LSMSelectStatement, SelectStatement>(LSMSelectStatement.class, SelectStatement.class) {
            public LSMParser createParser(LSMSource source) {
                return () -> prepareParser(source.getStream()).sqlQuery();
            }
        }), 
        prepareModel() 
    );
    
    private static SyntaxModel prepareModel() {
        SyntaxModel model = new SyntaxModel(prepareParser(CharStreams.fromString("")));
        model.introduce(SelectStatement.class);
        return model;
    }
    
    private static Sql92Parser prepareParser(CharStream input) {
        return new Sql92Parser(new CommonTokenStream(new Sql92Lexer(input)));
    }

    public static LSMDialect getInstance() {
        return dialect;
    }
}
