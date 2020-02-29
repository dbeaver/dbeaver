/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPPredicateRule;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;

import java.util.*;

/**
 * SQLRuleScanner.
 *
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class SQLRuleScanner extends RuleBasedScanner implements TPCharacterScanner {

    @NotNull
    private final IThemeManager themeManager;
    @NotNull
    private TreeMap<Integer, SQLScriptPosition> positions = new TreeMap<>();
    private Set<SQLScriptPosition> addedPositions = new HashSet<>();
    private Set<SQLScriptPosition> removedPositions = new HashSet<>();

    private final Map<TPToken, IToken> tokenMap = new IdentityHashMap<>();

    private boolean evalMode;
    private int keywordStyle = SWT.NORMAL;

    public SQLRuleScanner() {
        this.themeManager = PlatformUI.getWorkbench().getThemeManager();
    }

    public int getKeywordStyle() {
        return keywordStyle;
    }

    public void dispose() {
    }

    @NotNull
    public Collection<? extends Position> getPositions(int offset, int length) {
        return positions.subMap(offset, offset + length).values();
    }

    @NotNull
    public synchronized Set<SQLScriptPosition> getRemovedPositions(boolean clear) {
        Set<SQLScriptPosition> posList = removedPositions;
        if (clear) {
            removedPositions = new HashSet<>();
        }
        return posList;
    }

    @NotNull
    public synchronized Set<SQLScriptPosition> getAddedPositions(boolean clear) {
        Set<SQLScriptPosition> posList = addedPositions;
        if (clear) {
            addedPositions = new HashSet<>();
        }
        return posList;
    }

    public void refreshRules(@Nullable DBPDataSource dataSource, SQLRuleManager ruleManager) {
        tokenMap.clear();


        boolean boldKeywords = dataSource == null ?
                DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS) :
                dataSource.getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS);
        keywordStyle = boldKeywords ? SWT.BOLD : SWT.NORMAL;

        TPRule[] allRules = ruleManager.getAllRules();

        IRule[] result = new IRule[allRules.length];
        for (int i = 0; i < allRules.length; i++) {
            result[i] = adaptRule(allRules[i]);
        }
        setRules(result);
    }

    private IRule adaptRule(TPRule rule) {
        if (rule instanceof TPPredicateRule) {
            return new PredicateRuleAdapter((TPPredicateRule)rule);
        } else {
            return new SimpleRuleAdapter(rule);
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
    }

}
