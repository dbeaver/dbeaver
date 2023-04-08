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

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.Tree;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModelMappingResult;
import org.jkiss.dbeaver.model.lsm.sql.impl.SelectStatement;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.Sql92Parser;

import java.io.Reader;
import java.util.BitSet;

public class LSMEngine {

    private final LSMContext context;

    public LSMEngine(LSMContext context) {
        this.context = context;
    }

    public LSMContext getContext() {
        return context;
    }

    public LSMElement parseText(Reader input) throws LSMException {
        try {
            CodePointCharStream tokenStream = CharStreams.fromReader(input);

            Lexer lexer = context.getLexer();
            lexer.setInputStream(tokenStream);
            var tokens = new CommonTokenStream(lexer);
            tokens.fill();

            LSMParser parser = context.getParser();
            parser.setInputStream(tokens);

            parser.setBuildParseTree(true);
            parser.addErrorListener(new ParserErrorListener());

            var model = new SyntaxModel(parser);
            model.introduce(SelectStatement.class);
            // FIXME: we need some general parse result (LSMElement)
            Tree parseResult = ((Sql92Parser) parser).queryExpression();
            SyntaxModelMappingResult<SelectStatement> map = model.map(parseResult, SelectStatement.class);
            return map.getModel();

        } catch (Exception e) {
            throw new LSMException("Parser error", e);
        }
    }

    private static class ParserErrorListener implements ANTLRErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException re) {
            String sourceName = recognizer.getInputStream().getSourceName();
            if (!sourceName.isEmpty()) {
                sourceName = String.format("%s:%d:%d: ", sourceName, line, charPositionInLine);
            }

            //System.err.println(sourceName + " line " + line + ":" + charPositionInLine + " " + msg);
            //errorMsg = errorMsg + "\n" + sourceName + "line " + line + ":" + charPositionInLine + " " + msg;
        }

        @Override
        public void reportContextSensitivity(Parser arg0, DFA arg1, int arg2, int arg3, int arg4, ATNConfigSet arg5) {
            // just illustration of listeners possibility
        }

        @Override
        public void reportAttemptingFullContext(Parser arg0, DFA arg1, int arg2, int arg3, BitSet arg4, ATNConfigSet arg5) {
            // just illustration of listeners possibility
        }

        @Override
        public void reportAmbiguity(Parser arg0, DFA arg1, int arg2, int arg3, boolean arg4, BitSet arg5, ATNConfigSet arg6) {
            // just illustration of listeners possibility
        }
    }
}
