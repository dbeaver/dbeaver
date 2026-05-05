/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.sql.semantics.SQLDocumentSyntaxContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.text.parser.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * SQLRuleScanner.
 *
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class SQLRuleScanner extends RuleBasedScanner implements TPCharacterScanner {

    @NotNull
    private final IThemeManager themeManager;
    private final HashMap<SQLTokenType, IToken> extraSyntaxTokens = new HashMap<>();
    private SQLEditorBase editor = null;

    private final Map<TPToken, IToken> tokenMap = new IdentityHashMap<>();

    private int keywordStyle = SWT.NORMAL;

    private static final boolean DEBUG = false;

    public SQLRuleScanner() {
        this.themeManager = PlatformUI.getWorkbench().getThemeManager();
    }

    public int getKeywordStyle() {
        return keywordStyle;
    }

    public void dispose() {
    }

    public void refreshRules(@Nullable DBPDataSourceContainer dataSourceContainer, SQLRuleManager ruleManager, SQLEditorBase editor) {
        tokenMap.clear();
        boolean boldKeywords = dataSourceContainer == null ?
                DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS) :
                dataSourceContainer.getPreferenceStore().getBoolean(SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS);
        keywordStyle = boldKeywords ? SWT.BOLD : SWT.NORMAL;
        TPRule[] allRules = ruleManager.getAllRules();
        IRule[] result = new IRule[allRules.length];
        for (int i = 0; i < allRules.length; i++) {
            result[i] = adaptRule(allRules[i]);
        }
        setRules(result);
        this.editor = editor;
        this.extraSyntaxTokens.clear();
    }

    private IRule adaptRule(TPRule rule) {
        if (rule instanceof TPPredicateRule) {
            return new PredicateRuleAdapter((TPPredicateRule)rule);
        } else {
            return new SimpleRuleAdapter<>(rule);
        }
    }

    private IToken adaptToken(TPToken token) {
        if (token.isEOF()) {
            return Token.EOF;
        } else if (token.isUndefined()) {
            return Token.UNDEFINED;
        } else if (token.isWhitespace()) {
            return Token.WHITESPACE;
        } else {
            IToken jfToken = tokenMap.get(token);
            if (jfToken == null) {
                tokenMap.put(token, jfToken = new SQLTokenAdapter(token, this));
            }
            return jfToken;
        }
    }

    public Color getColor(String colorKey) {
        return getColor(colorKey, SWT.COLOR_BLACK);
    }

    private Color getColor(String colorKey, int colorDefault) {
        ITheme currentTheme = themeManager.getCurrentTheme();
        Color color = currentTheme.getColorRegistry().get(colorKey);
        if (color == null) {
            color = Display.getDefault().getSystemColor(colorDefault);
        }
        return color;
    }

    @Override
    public int getOffset() {
        return fOffset;
    }

    private static class LazyToken extends TPTokenDefault {
        public LazyToken(TPTokenType type) {
            super(type);
        }
    }
    
    private IToken tryResolveExtraToken() {
        SQLDocumentSyntaxContext syntaxContext = this.editor == null ? null : this.editor.getSyntaxContext();
        if (syntaxContext == null) {
            return Token.UNDEFINED;
        }
        
        int offset = this.getOffset();
        SQLQuerySymbolEntry entry = syntaxContext.findToken(offset);
        if (entry != null) {
            int end = syntaxContext.getLastAccessedTokenOffset() + entry.getInterval().length();
            if (end > offset) {
                if (DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    while (this.getOffset() < end) {
                        int c = super.read();
                        if (c == RuleBasedScanner.EOF) {
                            return Token.UNDEFINED;
                        }
                        sb.append((char) c);
                    }
                    System.out.println("found @" + offset + "-" + end + " " + entry + " = " + sb);
                } else {
                    while (this.getOffset() < end) {
                        if (super.read() == RuleBasedScanner.EOF) {
                            return Token.UNDEFINED;
                        }
                    }
                }
                return this.extraSyntaxTokens.computeIfAbsent(
                    entry.getSymbolClass().getTokenType(),
                    tt -> new SQLTokenAdapter(new LazyToken(tt), this)
                );
            } else {
                return Token.UNDEFINED;
            }
        } else {
            return Token.UNDEFINED;
        }
    }

    @Override
    public IToken nextToken() {
        super.fTokenOffset = fOffset;
        super.fColumn = UNDEFINED;

        IToken token = this.tryResolveExtraToken();
        if (!token.isUndefined()) {
            return token;
        }
        
        return super.nextToken();
    }

    private class SimpleRuleAdapter<RULE extends TPRule> implements IRule {
        protected final RULE rule;

        SimpleRuleAdapter(RULE rule) {
            this.rule = rule;
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner) {
            return adaptToken(rule.evaluate((TPCharacterScanner) scanner));
        }
    }

    private class PredicateRuleAdapter extends SimpleRuleAdapter<TPPredicateRule> implements IPredicateRule {
        PredicateRuleAdapter(TPPredicateRule rule) {
            super(rule);
        }

        @Override
        public IToken getSuccessToken() {
            return adaptToken(rule.getSuccessToken());
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner, boolean resume) {
            return adaptToken(rule.evaluate((TPCharacterScanner) scanner, resume));
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner) {
            return adaptToken(rule.evaluate((TPCharacterScanner) scanner));
        }

        @Override
        public String toString() {
            return "Adapter of [" + rule.toString() + "]";
        }
    }
}
