/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.rules.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLMultilineCommentToken;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.*;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>SQLPartitionScanner</code>, a subclass of a <code>RulesBasedPartitionScanner</code>,
 * is responsible for dynamically computing the partitions of its
 * SQL document as events signal that the document has
 * changed. The document partitions are based on tokens that represent comments
 * and SQL code sections.
 */
public class SQLPartitionScanner extends RuleBasedPartitionScanner implements TPPartitionScanner {

    private static final Log log = Log.getLog(SQLPartitionScanner.class);

    private final DBPDataSource dataSource;
    // Syntax highlight
    private final List<IPredicateRule> rules = new ArrayList<>();
    private final IToken commentToken = new Token(SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT);
    private final IToken multilineCommentToken = new Token(SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT);
    private final IToken sqlStringToken = new Token(SQLParserPartitions.CONTENT_TYPE_SQL_STRING);
    private final IToken sqlQuotedToken = new Token(SQLParserPartitions.CONTENT_TYPE_SQL_QUOTED);

    private void setupRules() {
        IPredicateRule[] result = new IPredicateRule[rules.size()];
        rules.toArray(result);
        setPredicateRules(result);
    }

    private void initRules(SQLDialect dialect, SQLRuleManager ruleManager) {
        TPRuleProvider ruleProvider = GeneralUtils.adapt(dialect, TPRuleProvider.class);
        if (ruleProvider != null) {
            List<TPRule> partRules = new ArrayList<>();
            ruleProvider.extendRules(
                dataSource == null ? null : dataSource.getContainer(),
                partRules,
                TPRuleProvider.RulePosition.PARTITION);
            for (TPRule pr : partRules) {
                if (pr instanceof TPPredicateRule) {
                    rules.add(new PredicateRuleAdapter((TPPredicateRule) pr));
                }
            }
        }

        adaptRules(ruleManager.getRulesByType(SQLTokenType.T_COMMENT));
        adaptRules(ruleManager.getRulesByType(SQLTokenType.T_QUOTED));
        adaptRules(ruleManager.getRulesByType(SQLTokenType.T_STRING));
    }

    private void adaptRules(@NotNull TPRule... rules) {
        for (TPRule rule : rules) {
            if (rule instanceof TPPredicateRule) {
                this.rules.add(new PredicateRuleAdapter((TPPredicateRule) rule));
            }
        }
    }

    public SQLPartitionScanner(DBPDataSource dataSource, SQLDialect dialect, SQLRuleManager ruleManager) {
        this.dataSource = dataSource;
        initRules(dialect, ruleManager);
        setupRules();
    }

    /**
     * Return the String ranging from the start of the current partition to the current scanning position. Some rules
     * (@see NestedMultiLineRule) need this information to calculate the comment nesting depth.
     *
     * @return value
     */
    public String getScannedPartitionString() {
        try {
            return fDocument.get(fPartitionOffset, fOffset - fPartitionOffset);
        } catch (Exception e) {
            // Do nothing
        }
        return "";
    }

    /**
     * Gets the partitions of the given document as an array of
     * <code>ITypedRegion</code> objects.  There is a distinct non-overlapping partition
     * for each comment line, string literal, delimited identifier, and "everything else"
     * (that is, SQL code other than a string literal or delimited identifier).
     *
     * @param doc the document to parse into partitions
     * @return an array containing the document partion regions
     */
    public static ITypedRegion[] getDocumentRegions(IDocument doc) {
        ITypedRegion[] regions = null;
        try {
            regions = TextUtilities.computePartitioning(doc, SQLParserPartitions.SQL_PARTITIONING, 0, doc.getLength(), false);
        } catch (BadLocationException e) {
            // ignore
        }

        return regions;
    }

    private class PredicateRuleAdapter implements IPredicateRule {
        private final TPPredicateRule rule;

        PredicateRuleAdapter(TPPredicateRule rule) {
            this.rule = rule;
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

    private IToken adaptToken(TPToken token) {
        if (token instanceof TPTokenDefault) {
            if (token.getData() instanceof SQLTokenType) {
                switch (((SQLTokenType) token.getData())) {
                    case T_STRING:
                        return sqlStringToken;
                    case T_QUOTED:
                        return sqlQuotedToken;
                    case T_COMMENT:
                        return token instanceof SQLMultilineCommentToken ? multilineCommentToken : commentToken;
                }
            }
        }
        return Token.UNDEFINED;
    }

}
