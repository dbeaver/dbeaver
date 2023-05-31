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
import org.jkiss.code.Nullable;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;

import java.util.Map;

public class Sql92Dialect {
    private static final Logger log = LoggerFactory.getLogger(Sql92Dialect.class);
    
    private static final LSMDialect dialect = new LSMDialectImpl(
        Map.of(LSMSelectStatement.class, new LSMAnalysisCaseImpl<>(LSMSelectStatement.class, SelectStatement.class) {

            @Nullable
            @Override
            public LSMParser createParser(LSMSource source) {
                return createParser(source, null);
            }

            @Nullable
            @Override
            public LSMParser createParser(@NotNull LSMSource source, @Nullable ANTLRErrorListener errorListener) {
                return () -> {
                    try {
                        return prepareParser(source.getStream(), errorListener).sqlQuery();
                    } catch (RecognitionException e) {
                        log.debug("Recognition exception occurred while trying to parse the query", e);
                        return null;
                    }
                };
            }
        }),
        prepareModel() 
    );
    
    @NotNull
    private static SyntaxModel prepareModel() {
        SyntaxModel model = new SyntaxModel(prepareParser(CharStreams.fromString(""), null));
        model.introduce(SelectStatement.class);
        return model;
    }

    @NotNull
    private static Sql92Parser prepareParser(@NotNull CharStream input, @Nullable ANTLRErrorListener errorListener) {
        Sql92Lexer lexer = new Sql92Lexer(input);
        if (errorListener != null) {
            lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
            lexer.addErrorListener(errorListener);
        }
        Sql92Parser parser =  new Sql92Parser(new CommonTokenStream(lexer));
        if (errorListener != null) {
            parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
            parser.addErrorListener(errorListener);
        }
        parser.getInterpreter().setPredictionMode(PredictionMode.LL);
        return parser;
    }

    @NotNull
    public static LSMDialect getInstance() {
        return dialect;
    }
}
