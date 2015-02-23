/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.rules.*;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>SQLPartitionScanner</code>, a subclass of a <code>RulesBasedPartitionScanner</code>,
 * is responsible for dynamically computing the partitions of its
 * SQL document as events signal that the document has
 * changed. The document partitions are based on tokens that represent comments
 * and SQL code sections.
 */
public class SQLPartitionScanner extends RuleBasedPartitionScanner {
    public final static String SQL_PARTITIONING = "___sql_partitioning";
    public final static String SQL_COMMENT = "sql_comment";
    public final static String SQL_MULTILINE_COMMENT = "sql_multiline_comment";
    //public final static String SQL_DOUBLE_QUOTES_IDENTIFIER = "sql_double_quotes_identifier";
    public final static String SQL_STRING = "sql_character";

    public final static String[] SQL_PARTITION_TYPES = new String[]{
        IDocument.DEFAULT_CONTENT_TYPE,
        SQL_COMMENT,
        SQL_MULTILINE_COMMENT,
        SQL_STRING,
        //SQL_DOUBLE_QUOTES_IDENTIFIER,
    };

    // Syntax higlight
    List<IPredicateRule> rules = new ArrayList<IPredicateRule>();
    IToken commentToken = new Token(SQL_COMMENT);
    IToken multilineCommentToken = new Token(SQL_MULTILINE_COMMENT);
    //IToken sqlIdentifierToken = new Token(SQL_DOUBLE_QUOTES_IDENTIFIER);
    IToken sqlStringToken = new Token(SQL_STRING);


    /**
     * Detector for empty comments.
     */
    static class EmptyCommentDetector implements IWordDetector {

        /*
         * @see IWordDetector#isWordStart
         */
        @Override
        public boolean isWordStart(char c)
        {
            return (c == '/');
        }

        /*
         * @see IWordDetector#isWordPart
         */
        @Override
        public boolean isWordPart(char c)
        {
            return (c == '*' || c == '/');
        }
    }

    /**
     * Word rule for empty comments.
     */
    static class EmptyCommentRule extends WordRule implements IPredicateRule {

        private IToken successToken;

        public EmptyCommentRule(IToken successToken)
        {
            super(new EmptyCommentDetector());
            this.successToken = successToken;
            addWord("/**/", this.successToken); //$NON-NLS-1$
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner, boolean resume)
        {
            return evaluate(scanner);
        }

        @Override
        public IToken getSuccessToken()
        {
            return successToken;
        }
    }

    /**
     * Constructor for SQLPartitionScanner. Creates rules to parse comment partitions in an SQL document. In the
     * constructor, is defined the entire set of rules used to parse the SQL document, in an instance of an
     * IPredicateRule. The coonstructor calls setPredicateRules method which associates the rules to the scanner and
     * makes the document ready for parsing.
     */
    public SQLPartitionScanner()
    {
        super();

        initRules(null);
        setupRules();
    }

    private void setupRules()
    {
        IPredicateRule[] result = new IPredicateRule[rules.size()];
        rules.toArray(result);
        setPredicateRules(result);
    }

    private void initRules(SQLSyntaxManager sqlSyntax)
    {
        /*
        //Add rule for identifier which is enclosed in double quotes.
        rules.add(new MultiLineRule("\"", "\"", sqlDoubleQuotesIdentifierToken, '\\'));

        //Add rule for SQL string.
        rules.add(new MultiLineRule("'", "'", sqlStringToken, '\\', true));

        //comments
        rules.add(new EndOfLineRule("--", commentToken));

        // Add special case word rule.
        EmptyCommentRule wordRule = new EmptyCommentRule(multilineCommentToken);
        rules.add(wordRule);
         */

        rules.add(new MultiLineRule(SQLConstants.STR_QUOTE_SINGLE, SQLConstants.STR_QUOTE_SINGLE, sqlStringToken, '\\'));

        for (String lineComment : sqlSyntax.getDialect().getSingleLineComments()) {
            rules.add(new EndOfLineRule(lineComment, commentToken));
        }

        // Add special case word rule.
        EmptyCommentRule wordRule = new EmptyCommentRule(multilineCommentToken);
        rules.add(wordRule);

        // Add rules for multi-line comments
        Pair<String, String> multiLineComments = sqlSyntax.getDialect().getMultiLineComments();
        if (multiLineComments != null) {
            rules.add(new MultiLineRule(multiLineComments.getFirst(), multiLineComments.getSecond(), multilineCommentToken, (char) 0, true));
        }
    }

    public SQLPartitionScanner(SQLSyntaxManager sqlSyntax)
    {
        initRules(sqlSyntax);
        //database specific rules
        setCommentsScanner(sqlSyntax);
        setupRules();

    }

    private void setCommentsScanner(SQLSyntaxManager sqlSyntax)
    {
        String[] singleLineComments = sqlSyntax.getDialect().getSingleLineComments();

        for (String singleLineComment : singleLineComments) {
            // Add rule for single line comments.
            rules.add(new EndOfLineRule(singleLineComment, commentToken));
        }
    }

    /**
     * Return the String ranging from the start of the current partition to the current scanning position. Some rules
     * (@see NestedMultiLineRule) need this information to calculate the comment nesting depth.
     * @return value
     */
    public String getScannedPartitionString()
    {
        try {
            return fDocument.get(fPartitionOffset, fOffset - fPartitionOffset);
        }
        catch (Exception e) {
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
    public static ITypedRegion[] getDocumentRegions(IDocument doc)
    {
        ITypedRegion[] regions = null;
        try {
            regions = TextUtilities.computePartitioning(doc, SQLPartitionScanner.SQL_PARTITIONING, 0, doc.getLength(),
                                                        false);
        }
        catch (BadLocationException e) {
            // ignore
        }

        return regions;
    }

}