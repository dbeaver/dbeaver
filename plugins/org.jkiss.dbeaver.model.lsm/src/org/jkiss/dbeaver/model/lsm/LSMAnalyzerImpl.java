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
package org.jkiss.dbeaver.model.lsm;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModelMappingResult;
import org.jkiss.dbeaver.model.lsm.sql.impl.SelectStatement;
import org.jkiss.dbeaver.model.stm.STMErrorListener;
import org.jkiss.dbeaver.model.stm.STMParserOverrides;
import org.jkiss.dbeaver.model.stm.STMSource;
import org.jkiss.dbeaver.model.stm.STMTreeRuleNode;
import org.jkiss.utils.Pair;
public abstract class LSMAnalyzerImpl<TLexer extends Lexer, TParser extends STMParserOverrides> implements LSMAnalyzer {

    private static final Log log = Log.getLog(LSMAnalyzerImpl.class);
    
    private final SyntaxModel syntaxModel;
    
    public LSMAnalyzerImpl() {
        Pair<TLexer, TParser> pair = this.createParser(STMSource.fromString(""));
        syntaxModel = new SyntaxModel(pair.getSecond());
        syntaxModel.introduce(SelectStatement.class);
    }

    @NotNull
    protected abstract Pair<TLexer, TParser> createParser(@NotNull STMSource source);

    @NotNull
    protected abstract STMTreeRuleNode parseSqlQueryImpl(@NotNull TParser parser);

    @NotNull
    protected TParser prepareParser(@NotNull STMSource source, @Nullable STMErrorListener errorListener) {
        Pair<TLexer, TParser> pair = this.createParser(source);
        TLexer lexer = pair.getFirst();
        TParser parser = pair.getSecond();
        
        if (errorListener != null) {
            lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
            lexer.addErrorListener((ANTLRErrorListener) errorListener);
            parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
            parser.addErrorListener((ANTLRErrorListener) errorListener);
        }
        
        parser.getInterpreter().setPredictionMode(PredictionMode.LL);
        
        return parser;
    }

    @Nullable
    @Override
    public STMTreeRuleNode parseSqlQueryTree(@NotNull STMSource source, @Nullable STMErrorListener errorListener) {
        try {
            TParser parser = prepareParser(source, errorListener);
            STMTreeRuleNode result = parseSqlQueryImpl(parser);
            result.fixup(parser);
            return result;
        } catch (RecognitionException e) {
            log.debug("Recognition exception occurred while trying to parse the query", e);
            return null;
        }
    }

    @Nullable
    @Override
    public LSMElement parseSqlQueryModel(@NotNull STMSource source) {
        STMTreeRuleNode root = parseSqlQueryTree(source, null);
        if (root != null) {
            SyntaxModelMappingResult<SelectStatement> result = this.syntaxModel.map(root, SelectStatement.class);
            if (!result.isNoErrors()) {
                result.getErrors().printToStderr();
            }
            return result.getModel();
        }
        return null;
    }
}
