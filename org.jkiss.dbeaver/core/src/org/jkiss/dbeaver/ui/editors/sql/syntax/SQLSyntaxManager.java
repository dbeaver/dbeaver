/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.*;

/**
 * SQLSyntaxManager.
 *
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class SQLSyntaxManager extends RuleBasedScanner implements IPropertyChangeListener
{
    static final Log log = LogFactory.getLog(SQLSyntaxManager.class);

    public enum KeywordType {
        KEYWORD,
        FUNCTION,
        TYPE
    }

    public static final String CONFIG_COLOR_KEYWORD = "org.jkiss.dbeaver.sql.editor.color.keyword.foreground";
    public static final String CONFIG_COLOR_DATATYPE = "org.jkiss.dbeaver.sql.editor.color.datatype.foreground";
    public static final String CONFIG_COLOR_STRING = "org.jkiss.dbeaver.sql.editor.color.string.foreground";
    public static final String CONFIG_COLOR_NUMBER = "org.jkiss.dbeaver.sql.editor.color.number.foreground";
    public static final String CONFIG_COLOR_COMMENT = "org.jkiss.dbeaver.sql.editor.color.comment.foreground";
    public static final String CONFIG_COLOR_DELIMITER = "org.jkiss.dbeaver.sql.editor.color.delimiter.foreground";
    public static final String CONFIG_COLOR_TEXT = "org.jkiss.dbeaver.sql.editor.color.text.foreground";
    public static final String CONFIG_COLOR_BACKGROUND = "org.jkiss.dbeaver.sql.editor.color.text.background";

    private IThemeManager themeManager;

    private TreeMap<String, KeywordType> allKeywords = new TreeMap<String, KeywordType>();

    private TreeSet<String> reservedWords = new TreeSet<String>();
    private TreeSet<String> functions = new TreeSet<String>();
    private TreeSet<String> types = new TreeSet<String>();
    private TreeSet<String> tableQueryWords = new TreeSet<String>();
    private TreeSet<String> columnQueryWords = new TreeSet<String>();

    private String catalogSeparator;
    private String statementDelimiter = ";";
    private String[] singleLineComments = { "--" };

    private TreeMap<Integer, SQLScriptPosition> positions = new TreeMap<Integer, SQLScriptPosition>();

    private Set<SQLScriptPosition> addedPositions = new HashSet<SQLScriptPosition>();
    private Set<SQLScriptPosition> removedPositions = new HashSet<SQLScriptPosition>();

    public SQLSyntaxManager(IWorkbenchPart workbenchPart)
    {
        themeManager = workbenchPart.getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
        themeManager.addPropertyChangeListener(this);
    }

    public void dispose()
    {
        themeManager.removePropertyChangeListener(this);
    }

    public Collection<? extends Position> getPositions(int offset, int length)
    {
        return positions.subMap(offset, offset + length).values();
    }

    public synchronized Set<SQLScriptPosition> getRemovedPositions(boolean clear)
    {
        Set<SQLScriptPosition> posList = removedPositions;
        if (clear) {
            removedPositions = new HashSet<SQLScriptPosition>();
        }
        return posList;
    }

    public synchronized Set<SQLScriptPosition> getAddedPositions(boolean clear)
    {
        Set<SQLScriptPosition> posList = addedPositions;
        if (clear) {
            addedPositions = new HashSet<SQLScriptPosition>();
        }
        return posList;
    }

    public String getCatalogSeparator()
    {
        return catalogSeparator;
    }

    public boolean isLoaded()
    {
        return !allKeywords.isEmpty();
    }

    public void changeDataSource(DBPDataSource dataSource)
    {
        DBPDataSourceInfo dataSourceInfo = null;
        if (dataSource != null) {
            dataSourceInfo = dataSource.getInfo();
        }
        catalogSeparator = ".";
        String additional = dataSourceInfo == null ? "" : dataSourceInfo.getCatalogSeparator();
        if (!CommonUtils.isEmpty(additional) && !additional.equals(catalogSeparator)) {
            catalogSeparator += additional;
        }
        loadSyntax(dataSourceInfo);
        changeRules();
    }

    public Set<String> getReservedWords()
    {
        return reservedWords;
    }

    public Set<String> getFunctions()
    {
        return functions;
    }

    public TreeSet<String> getTypes()
    {
        return types;
    }

    public KeywordType getKeywordType(String word)
    {
        return allKeywords.get(word);
    }

    public List<String> getMatchedKeywords(String word)
    {
        word = word.toUpperCase();
        List<String> result = new ArrayList<String>();
        for (String keyword : allKeywords.tailMap(word).keySet()) {
            if (keyword.startsWith(word)) {
                result.add(keyword);
            } else {
                break;
            }
        }
        return result;
    }

    public boolean isKeywordStart(String word)
    {
        SortedMap<String,KeywordType> map = allKeywords.tailMap(word);
        return !map.isEmpty() && map.firstKey().startsWith(word);
    }

    public boolean isTableQueryWord(String word)
    {
        return tableQueryWords.contains(word.toUpperCase());
    }

    public boolean isColumnQueryWord(String word)
    {
        return columnQueryWords.contains(word.toUpperCase());
    }

    public String getStatementDelimiter()
    {
        return statementDelimiter;
    }

    private void loadSyntax(DBPDataSourceInfo dataSourceInfo)
    {
        allKeywords.clear();
        reservedWords.clear();
        functions.clear();
        types.clear();

        // Add default set of keywords
        reservedWords.addAll(Arrays.asList(DEFAULT_KEYWORDS));
        types.addAll(Arrays.asList(DEFAULT_TYPES));
        tableQueryWords.addAll(Arrays.asList(TABLE_KEYWORDS));
        columnQueryWords.addAll(Arrays.asList(COLUMN_KEYWORDS));

        if (dataSourceInfo != null) {
            // Keywords
            List<String> sqlKeywords = dataSourceInfo.getSQLKeywords();
            if (sqlKeywords != null) {
                for (String keyword : dataSourceInfo.getSQLKeywords()) {
                    reservedWords.add(keyword.toUpperCase());
                }
            }
            // Functions
            Set<String> allFunctions = new HashSet<String>();
            if (dataSourceInfo.getNumericFunctions() != null) {
                for (String func : dataSourceInfo.getNumericFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            if (dataSourceInfo.getStringFunctions() != null) {
                for (String func : dataSourceInfo.getStringFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            if (dataSourceInfo.getSystemFunctions() != null) {
                for (String func : dataSourceInfo.getSystemFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            if (dataSourceInfo.getTimeDateFunctions() != null) {
                for (String func : dataSourceInfo.getTimeDateFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            functions.addAll(allFunctions);

            // Types
            List<DBSDataType> supportedDataTypes = dataSourceInfo.getSupportedDataTypes();
            if (supportedDataTypes != null) {
                for (DBSDataType dataType : dataSourceInfo.getSupportedDataTypes()) {
                    types.add(dataType.getName().toUpperCase());
                }
            }
            functions.addAll(allFunctions);
        }

        if (types .isEmpty()) {
            // Add default types
        }

        // Remove types and functions from reserved words list
        reservedWords.removeAll(types);
        reservedWords.removeAll(functions);

        addKeywords(reservedWords, KeywordType.KEYWORD);
        addKeywords(functions, KeywordType.FUNCTION);
        addKeywords(types, KeywordType.TYPE);
    }

    private void addKeywords(Set<String> set, KeywordType type)
    {
        for (String keyword : set) {
            allKeywords.put(keyword, type);
        }
    }

    private void changeRules()
    {
        Color backgroundColor = getColor(SQLSyntaxManager.CONFIG_COLOR_BACKGROUND, SWT.COLOR_WHITE);
        IToken keywordToken = new Token(new TextAttribute(
            getColor(SQLSyntaxManager.CONFIG_COLOR_KEYWORD),
            backgroundColor,
            SWT.BOLD));
        IToken typeToken = new Token(new TextAttribute(
            getColor(SQLSyntaxManager.CONFIG_COLOR_DATATYPE),
            backgroundColor,
            SWT.BOLD));
        IToken stringToken = new Token(new TextAttribute(
            getColor(SQLSyntaxManager.CONFIG_COLOR_STRING)));
        IToken numberToken = new Token(new TextAttribute(
            getColor(SQLSyntaxManager.CONFIG_COLOR_NUMBER)));
        IToken commentToken = new Token(new TextAttribute(
            getColor(SQLSyntaxManager.CONFIG_COLOR_COMMENT)));
        SQLDelimiterToken delimToken = new SQLDelimiterToken(new TextAttribute(
            getColor(SQLSyntaxManager.CONFIG_COLOR_DELIMITER, SWT.COLOR_RED)));
        IToken otherToken = new Token(new TextAttribute(
            getColor(SQLSyntaxManager.CONFIG_COLOR_TEXT)));

        setDefaultReturnToken(otherToken);
        List<IRule> rules = new ArrayList<IRule>();

        // Add rule for single-line comments.
        rules.add( new EndOfLineRule( "--", commentToken )); //$NON-NLS-1$

        // Add rules for delimited identifiers and string literals.
        rules.add( new SingleLineRule( "'", "'", stringToken, (char) 0 )); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        rules.add( new SingleLineRule( "\"", "\"", stringToken, (char) 0 )); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Add rules for multi-line comments
        rules.add(new NestedMultiLineRule("/*", "*/", commentToken, (char) 0, true));

        // Add generic whitespace rule.
        rules.add( new WhitespaceRule( new SQLWhiteSpaceDetector() ));

        // Add numeric rule
        rules.add(new NumberRule(numberToken));

        // Add word rule for keywords, types, and constants.
        WordRule wordRule = new WordRule(new SQLWordDetector(), otherToken, true);
        for (String reserverWord : getReservedWords()) {
            wordRule.addWord(reserverWord, keywordToken);
        }
        for (String function : getFunctions()) {
            wordRule.addWord(function, typeToken);
        }
        for (String type : getTypes()) {
            wordRule.addWord(type, typeToken);
        }
        rules.add(wordRule);

        {
            // Delimiter rule
            WordRule delimRule = new WordRule(new IWordDetector() {
                public boolean isWordStart(char c)
                {
                    return statementDelimiter.charAt(0) == c;
                }

                public boolean isWordPart(char c)
                {
                    return statementDelimiter.indexOf(c) != -1;
                }
            });
            delimRule.addWord(statementDelimiter, delimToken);
            rules.add(delimRule);
        }

        IRule[] result = new IRule[rules.size()];
        rules.toArray(result);
        setRules(result);
    }

    public String[] getSingleLineComments()
    {
        return singleLineComments;
    }

    public Color getColor(String colorKey)
    {
        return getColor(colorKey, SWT.COLOR_BLACK);
    }

    public Color getColor(String colorKey, int colorDefault)
    {
        ITheme currentTheme = themeManager.getCurrentTheme();
        Color color = currentTheme.getColorRegistry().get(colorKey);
        if (color == null) {
            color = Display.getDefault().getSystemColor(colorDefault);
        }
        return color;
    }

    public void propertyChange(PropertyChangeEvent event)
    {
    }

    private static final String[] TABLE_KEYWORDS = {
        "FROM",
        "UPDATE",
        "INTO",
		"TABLE"
    };

    private static final String[] COLUMN_KEYWORDS = {
        "SELECT",
        "WHERE",
        "SET",
        "ON"
    };

    private static final String[] DEFAULT_KEYWORDS = {
        "ALL",
        "ALTER",
        "AND",
        "ANY",
        "AS",
        "ASC",
        "AT",
        "AVG",
        "BEGIN",
        "BETWEEN",
        "BOTH",
        "BY", 
        "CASE",
        "CAST",
        "CHECK",
        "COLUMN",
        "COMMIT",
        "CONNECT",
        "CONSTRAINT",
        "CONSTRAINTS",
        "CONTINUE",
        "COUNT",
        "CREATE",
        "CURSOR",
        "DATABASE",
        "DEFAULT",
        "DELETE",
        "DESC",
        "DISCONNECT",
        "DISTINCT",
        "DROP",
        "ELSE",
        "END",
        "END-EXEC",
        "EXECUTE",
        "EXISTS",
        "FOR",
        "FOREIGN",
        "FROM",
        "GRANT",
        "GROUP",
        "HAVING",
        "IN",
        "INDEX",
        "INNER",
        "INSERT",
        "INTERSECT",
        "INTO",
        "IS",
        "JOIN",
        "KEY",
        "LEFT",
        "LIKE",
        "MAX",
        "MIN",
        "NEXT",
        "NOT",
        "NULL",
        "OF",
        "ON",
        "OR",
        "ORDER",
        "OUTER",
        "PREPARE",
        "PRIMARY",
        "PROCEDURE",
        "REFERENCES",
        "REVOKE",
        "RIGHT",
        "ROLLBACK",
        "SELECT",
        "SET",
        "SUM",
        "TABLE",
        "TABLESPACE",
        "THEN",
        "TO",
        "UNION",
        "UNIQUE",
        "UPDATE",
        "USING",
        "VALUES",
        "VIEW",
        "WHEN",
        "WHERE",
        "WITH",
    };

    private static final String[] DEFAULT_TYPES = {
        "CHAR",
        "VARCHAR",
        "INTEGER",
        "FLOAT",
        "DATE",
        "TIME",
        "TIMESTAMP",
    };

}
