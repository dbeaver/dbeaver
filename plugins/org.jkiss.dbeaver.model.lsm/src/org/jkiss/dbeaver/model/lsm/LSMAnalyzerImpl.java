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
package org.jkiss.dbeaver.model.lsm;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.stm.STMErrorListener;
import org.jkiss.dbeaver.model.stm.STMParserOverrides;
import org.jkiss.dbeaver.model.stm.STMSkippingErrorListener;
import org.jkiss.dbeaver.model.stm.STMSource;
import org.jkiss.dbeaver.model.stm.STMTreeRuleNode;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public abstract class LSMAnalyzerImpl<TLexer extends Lexer, TParser extends STMParserOverrides> implements LSMAnalyzer {

    private static final Log log = Log.getLog(LSMAnalyzerImpl.class);
    
    private final LSMAnalyzerParameters parameters;
    
    public LSMAnalyzerImpl(@NotNull LSMAnalyzerParameters parameters) {
        this.parameters = parameters;
    }

    @NotNull
    protected abstract Pair<TLexer, TParser> createParser(@NotNull STMSource source, @NotNull LSMAnalyzerParameters parameters);

    /**
     * Creates a parser over an already lexed token stream. Subclasses that do not support token reuse
     * may return {@code null}; the analyzer then falls back to re-creating the lexer and parser.
     */
    @Nullable
    protected TParser createParser(
        @NotNull TokenStream tokenStream,
        @NotNull LSMAnalyzerParameters parameters
    ) {
        return null;
    }

    @NotNull
    protected abstract STMTreeRuleNode parseSqlQueryImpl(@NotNull TParser parser);

    @NotNull
    protected TParser prepareParser(@NotNull STMSource source, @Nullable STMErrorListener errorListener) {
        Pair<TLexer, TParser> pair = this.createParser(source, this.parameters);
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
    private TParser prepareParserForReuse(
        @NotNull BufferedTokenStream bufferedTokens,
        @Nullable STMErrorListener errorListener
    ) {
        TokenSource tokenSource = bufferedTokens.getTokenSource();
        if (!(tokenSource instanceof Lexer lexer)) {
            // Without a lexer to reconfigure, the SLL listener would stay attached and later lexer errors
            // would never reach the caller. Force a clean re-lex instead.
            return null;
        }
        TParser parser = this.createParser(bufferedTokens, this.parameters);
        if (parser == null) {
            return null;
        }
        configureErrorListeners(parser, errorListener);
        configureErrorListeners(lexer, errorListener);
        parser.getInterpreter().setPredictionMode(PredictionMode.LL);
        return parser;
    }

    private static void configureErrorListeners(
        @NotNull Recognizer<?, ?> recognizer,
        @Nullable STMErrorListener errorListener
    ) {
        recognizer.removeErrorListeners();
        if (errorListener != null) {
            recognizer.addErrorListener(errorListener);
        } else {
            recognizer.addErrorListener(ConsoleErrorListener.INSTANCE);
        }
    }

    @Nullable
    @Override
    public STMTreeRuleNode parseSqlQueryTree(@NotNull STMSource source, @Nullable STMErrorListener errorListener) {
        // Do not report SLL-only errors to the caller: LL parsing may still succeed on the same input.
        SllErrorListener sllErrors = new SllErrorListener();
        TParser sllParser = null;
        try {
            sllParser = prepareParser(source, sllErrors);
            // Abort the tentative pass at the first syntax error: LL may still parse the same input,
            // and full error recovery would make unsupported statements pay for two complete parses.
            sllParser.setErrorHandler(new SllBailErrorStrategy());
            sllParser.getInterpreter().setPredictionMode(PredictionMode.SLL);
            STMTreeRuleNode result = parseSqlQueryImpl(sllParser);
            if (sllParser.getNumberOfSyntaxErrors() == 0
                && sllErrors.getErrorCount() == 0
                && !result.hasErrorChildren()
            ) {
                result.fixup(sllParser);
                return result;
            }
            log.debug("SLL parsing produced syntax errors or left error nodes, falling back to LL");
        } catch (SllBailException e) {
            log.debug("SLL parsing aborted at the first syntax error, falling back to LL");
        } catch (RecognitionException e) {
            log.debug("SLL parsing failed, falling back to LL", e);
        }

        // Reuse the already lexed tokens whenever possible. This avoids tokenizing the same input twice
        // on the LL fallback.
        if (sllParser != null && sllParser.getTokenStream() instanceof BufferedTokenStream bufferedTokens) {
            TParser llParser = prepareParserForReuse(bufferedTokens, errorListener);
            if (llParser != null) {
                try {
                    // The SLL trial does not forward errors to the caller, so replay the lexer errors it collected.
                    // Errors from tokens not lexed yet will be reported by the reused lexer during the LL parse.
                    sllErrors.replayLexerErrors(
                        errorListener != null ? errorListener : ConsoleErrorListener.INSTANCE
                    );
                    bufferedTokens.seek(0);
                    STMTreeRuleNode result = parseSqlQueryImpl(llParser);
                    result.fixup(llParser);
                    return result;
                } catch (RecognitionException e) {
                    log.debug("Recognition exception occurred while trying to parse the query", e);
                    return null;
                }
            }
        }

        try {
            // The source stream may be reused by readers, so rewind it before the fallback parse.
            source.getStream().seek(0);
            TParser parser = prepareParser(source, errorListener);
            parser.getInterpreter().setPredictionMode(PredictionMode.LL);
            STMTreeRuleNode result = parseSqlQueryImpl(parser);
            result.fixup(parser);
            return result;
        } catch (RecognitionException e) {
            log.debug("Recognition exception occurred while trying to parse the query", e);
            return null;
        }
    }

    /**
     * Error listener for the tentative SLL pass. Parser errors are only counted, while lexer errors are also
     * recorded: the LL pass may reuse the token stream and would otherwise never report them.
     */
    private static final class SllErrorListener extends STMSkippingErrorListener {
        @Nullable
        private List<LexerError> lexerErrors;

        @Override
        public void syntaxError(
            @NotNull Recognizer<?, ?> recognizer,
            @Nullable Object offendingSymbol,
            int line,
            int charPositionInLine,
            @NotNull String msg,
            @Nullable RecognitionException e
        ) {
            super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
            if (recognizer instanceof Lexer) {
                if (lexerErrors == null) {
                    lexerErrors = new ArrayList<>();
                }
                lexerErrors.add(new LexerError(recognizer, offendingSymbol, line, charPositionInLine, msg, e));
            }
        }

        void replayLexerErrors(@NotNull ANTLRErrorListener errorListener) {
            if (lexerErrors != null) {
                for (LexerError error : lexerErrors) {
                    errorListener.syntaxError(
                        error.recognizer(),
                        error.offendingSymbol(),
                        error.line(),
                        error.charPositionInLine(),
                        error.message(),
                        error.exception()
                    );
                }
            }
        }

        private record LexerError(
            @NotNull Recognizer<?, ?> recognizer,
            @Nullable Object offendingSymbol,
            int line,
            int charPositionInLine,
            @NotNull String message,
            @Nullable RecognitionException exception
        ) {
        }
    }

    /**
     * Bail strategy used only for the tentative SLL pass. SLL failures are regular control flow here,
     * so avoid allocating a stack trace for every fallback.
     */
    private static final class SllBailErrorStrategy extends BailErrorStrategy {
        @Override
        public void recover(@NotNull Parser recognizer, @NotNull RecognitionException e) {
            throw new SllBailException();
        }

        @NotNull
        @Override
        public Token recoverInline(@NotNull Parser recognizer) throws RecognitionException {
            throw new SllBailException();
        }
    }

    private static final class SllBailException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private SllBailException() {
            super(null, null, false, false);
        }
    }
}
