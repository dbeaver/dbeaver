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
package org.jkiss.dbeaver.model.lsm.impl;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
import org.jkiss.dbeaver.model.lsm.LSMElement;
import org.jkiss.dbeaver.model.lsm.LSMSource;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModelMappingResult;
import org.jkiss.dbeaver.model.lsm.sql.impl.SelectStatement;
import org.jkiss.dbeaver.model.stm.ParserOverrides;
import org.jkiss.dbeaver.model.stm.TreeRuleNode;
import org.jkiss.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LSMAnalyzerImpl<TLexer extends Lexer, TParser extends ParserOverrides> implements LSMAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(LSMAnalyzerImpl.class);
    
    private final SyntaxModel syntaxModel;
    
    public LSMAnalyzerImpl() {
        Pair<TLexer, TParser> pair = this.createParser(LSMSource.fromString(""));
        syntaxModel = new SyntaxModel(pair.getSecond());
        syntaxModel.introduce(SelectStatement.class);
    }

    @NotNull
    protected abstract Pair<TLexer, TParser> createParser(@NotNull LSMSource source);

    @NotNull
    protected abstract TreeRuleNode parseSqlQueryImpl(@NotNull TParser parser);

    @NotNull
    private TParser prepareParser(@NotNull LSMSource source, @Nullable ANTLRErrorListener errorListener) {
        Pair<TLexer, TParser> pair = this.createParser(source);
        TLexer lexer = pair.getFirst();
        TParser parser = pair.getSecond();
        
        if (errorListener != null) {
            lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
            lexer.addErrorListener(errorListener);
            parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
            parser.addErrorListener(errorListener);
        }
        
        parser.getInterpreter().setPredictionMode(PredictionMode.LL);
        
        return parser;
    }

    @Nullable
    @Override
    public TreeRuleNode parseSqlQueryTree(@NotNull LSMSource source, @Nullable ANTLRErrorListener errorListener) {
        try {
            TParser parser = prepareParser(source, errorListener);
            TreeRuleNode result = parseSqlQueryImpl(parser);
            result.fixup(parser);
            return result;
        } catch (RecognitionException e) {
            log.debug("Recognition exception occurred while trying to parse the query", e);
            return null;
        }
    }

    @NotNull
    @Override
    public LSMElement parseSqlQueryModel(@NotNull LSMSource source) {
        SyntaxModelMappingResult<SelectStatement> result = this.syntaxModel.map(parseSqlQueryTree(source, null), SelectStatement.class);
        if (!result.isNoErrors()) {
            result.getErrors().printToStderr();
        }
        return result.getModel();
    }
}
