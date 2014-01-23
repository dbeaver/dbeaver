/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLKeywordManager;
import org.jkiss.dbeaver.model.sql.SQLStateType;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * SQL Dialect JDBC API implementation
 */
public class JDBCSQLDialect implements SQLDialect {

    static final Log log = LogFactory.getLog(JDBCSQLDialect.class);

    private final JDBCDataSource dataSource;
    private String name;
    private String identifierQuoteString;
    private SQLStateType sqlStateType;
    private List<String> sqlKeywords;
    private List<String> numericFunctions;
    private List<String> stringFunctions;
    private List<String> systemFunctions;
    private List<String> timeDateFunctions;
    private String searchStringEscape;
    private String catalogSeparator;
    private boolean isCatalogAtStart;
    private int catalogUsage;
    private int schemaUsage;
    private String validCharacters;
    private boolean supportsUnquotedMixedCase;
    private boolean supportsQuotedMixedCase;
    private DBPIdentifierCase unquotedIdentCase;
    private DBPIdentifierCase quotedIdentCase;
    private boolean supportsSubqueries = false;
    private SQLKeywordManager keywordManager;

    public JDBCSQLDialect(JDBCDataSource dataSource, String name, JDBCDatabaseMetaData metaData)
    {
        this.dataSource = dataSource;
        this.name = name;
        //this.keywordManager = new JDBCSQLKeywordManager(dataSource);

        try {
            this.identifierQuoteString = metaData.getIdentifierQuoteString();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.identifierQuoteString = null;
        }
        if (identifierQuoteString != null) {
            identifierQuoteString = identifierQuoteString.trim();
        }
        if (identifierQuoteString != null && identifierQuoteString.isEmpty()) {
            identifierQuoteString = null;
        }

        try {
            switch (metaData.getSQLStateType()) {
                case DatabaseMetaData.sqlStateXOpen:
                    this.sqlStateType = SQLStateType.XOPEN;
                    break;
                case DatabaseMetaData.sqlStateSQL99:
                    this.sqlStateType = SQLStateType.SQL99;
                    break;
                default:
                    this.sqlStateType = SQLStateType.UNKNOWN;
                    break;
            }
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.sqlStateType = SQLStateType.UNKNOWN;
        }

        try {
            supportsSubqueries = metaData.supportsCorrelatedSubqueries();
        } catch (SQLException e) {
            log.debug(e);
        }

        try {
            this.supportsUnquotedMixedCase = metaData.supportsMixedCaseIdentifiers();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            this.supportsUnquotedMixedCase = false;
        }
        try {
            this.supportsQuotedMixedCase = metaData.supportsMixedCaseQuotedIdentifiers();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            this.supportsQuotedMixedCase = false;
        }
        try {
            if (metaData.storesUpperCaseIdentifiers()) {
                this.unquotedIdentCase = DBPIdentifierCase.UPPER;
            } else if (metaData.storesLowerCaseIdentifiers()) {
                this.unquotedIdentCase = DBPIdentifierCase.LOWER;
            } else {
                this.unquotedIdentCase = DBPIdentifierCase.MIXED;
            }
        } catch (SQLException e) {
            log.debug(e.getMessage());
            this.unquotedIdentCase = DBPIdentifierCase.MIXED;
        }
        try {
            if (metaData.storesUpperCaseQuotedIdentifiers()) {
                this.quotedIdentCase = DBPIdentifierCase.UPPER;
            } else if (metaData.storesLowerCaseQuotedIdentifiers()) {
                this.quotedIdentCase = DBPIdentifierCase.LOWER;
            } else {
                this.quotedIdentCase = DBPIdentifierCase.MIXED;
            }
        } catch (SQLException e) {
            log.debug(e.getMessage());
            this.quotedIdentCase = DBPIdentifierCase.MIXED;
        }
        try {
            this.sqlKeywords = makeStringList(metaData.getSQLKeywords());
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.sqlKeywords = new ArrayList<String>();
        }
        try {
            this.numericFunctions = makeStringList(metaData.getNumericFunctions());
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.numericFunctions = Collections.emptyList();
        }
        try {
            this.stringFunctions = makeStringList(metaData.getStringFunctions());
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.stringFunctions = Collections.emptyList();
        }
        try {
            this.systemFunctions = makeStringList(metaData.getSystemFunctions());
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.systemFunctions = Collections.emptyList();
        }
        try {
            this.timeDateFunctions = makeStringList(metaData.getTimeDateFunctions());
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.timeDateFunctions = Collections.emptyList();
        }
        try {
            this.searchStringEscape = metaData.getSearchStringEscape();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.searchStringEscape = "\\"; //$NON-NLS-1$
        }
        try {
            this.catalogSeparator = metaData.getCatalogSeparator();
            if (CommonUtils.isEmpty(this.catalogSeparator)) {
                this.catalogSeparator = String.valueOf(SQLConstants.STRUCT_SEPARATOR);
            }
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.catalogSeparator = String.valueOf(SQLConstants.STRUCT_SEPARATOR);
        }
        try {
            catalogUsage =
                (metaData.supportsCatalogsInDataManipulation() ? SQLDialect.USAGE_DML : 0) |
                    (metaData.supportsCatalogsInTableDefinitions() ? SQLDialect.USAGE_DDL : 0) |
                    (metaData.supportsCatalogsInProcedureCalls() ? SQLDialect.USAGE_PROC : 0) |
                    (metaData.supportsCatalogsInIndexDefinitions() ? SQLDialect.USAGE_INDEX : 0) |
                    (metaData.supportsCatalogsInPrivilegeDefinitions() ? SQLDialect.USAGE_PRIV : 0);
        } catch (SQLException e) {
            log.debug(e.getMessage());
            catalogUsage = SQLDialect.USAGE_NONE;
        }
        try {
            schemaUsage =
                (metaData.supportsSchemasInDataManipulation() ? SQLDialect.USAGE_DML : 0) |
                    (metaData.supportsSchemasInTableDefinitions() ? SQLDialect.USAGE_DDL : 0) |
                    (metaData.supportsSchemasInProcedureCalls() ? SQLDialect.USAGE_PROC : 0) |
                    (metaData.supportsSchemasInIndexDefinitions() ? SQLDialect.USAGE_INDEX : 0) |
                    (metaData.supportsSchemasInPrivilegeDefinitions() ? SQLDialect.USAGE_PRIV : 0);
        } catch (SQLException e) {
            log.debug(e.getMessage());
            schemaUsage = SQLDialect.USAGE_NONE;
        }
        try {
            validCharacters = metaData.getExtraNameCharacters();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            validCharacters = ""; //$NON-NLS-1$
        }

        try {
            this.isCatalogAtStart = metaData.isCatalogAtStart();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.isCatalogAtStart = true;
        }

    }

    @Override
    public String getDialectName() {
        return name;
    }

    @Override
    public String getIdentifierQuoteString()
    {
        return identifierQuoteString;
    }

    @Override
    public Collection<String> getSQLKeywords()
    {
        return sqlKeywords;
    }

    public void addSQLKeyword(String keyword)
    {
        sqlKeywords.add(keyword);
    }

    @Override
    public Collection<String> getNumericFunctions()
    {
        return numericFunctions;
    }

    @Override
    public Collection<String> getStringFunctions()
    {
        return stringFunctions;
    }

    @Override
    public Collection<String> getSystemFunctions()
    {
        return systemFunctions;
    }

    @Override
    public Collection<String> getTimeDateFunctions()
    {
        return timeDateFunctions;
    }

    @Override
    public Collection<String> getExecuteKeywords()
    {
        return null;
    }

    @Override
    public SQLKeywordManager getKeywordManager() {
        if (keywordManager == null) {
            keywordManager = new JDBCSQLKeywordManager(dataSource);
        }
        return keywordManager;
    }

    @Override
    public String getSearchStringEscape()
    {
        return searchStringEscape;
    }

    @Override
    public int getCatalogUsage()
    {
        return catalogUsage;
    }

    @Override
    public int getSchemaUsage()
    {
        return schemaUsage;
    }

    @Override
    public String getCatalogSeparator()
    {
        return catalogSeparator;
    }

    @Override
    public char getStructSeparator()
    {
        return SQLConstants.STRUCT_SEPARATOR;
    }

    @Override
    public boolean isCatalogAtStart()
    {
        return isCatalogAtStart;
    }

    @Override
    public SQLStateType getSQLStateType()
    {
        return sqlStateType;
    }

    @Override
    public String getScriptDelimiter()
    {
        return ";"; //$NON-NLS-1$
    }

    @Override
    public boolean validUnquotedCharacter(char c)
    {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_' || validCharacters.indexOf(c) != -1;
    }

    @Override
    public boolean supportsUnquotedMixedCase()
    {
        return supportsUnquotedMixedCase;
    }

    @Override
    public boolean supportsQuotedMixedCase()
    {
        return supportsQuotedMixedCase;
    }

    @Override
    public DBPIdentifierCase storesUnquotedCase()
    {
        return unquotedIdentCase;
    }

    @Override
    public DBPIdentifierCase storesQuotedCase()
    {
        return quotedIdentCase;
    }

    @Override
    public boolean supportsSubqueries()
    {
        return supportsSubqueries;
    }

    public void setSupportsSubqueries(boolean supportsSubqueries)
    {
        this.supportsSubqueries = supportsSubqueries;
    }

    private static List<String> makeStringList(String source)
    {
        List<String> result = new ArrayList<String>();
        if (source != null && source.length() > 0) {
            StringTokenizer st = new StringTokenizer(source, ";,"); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                result.add(st.nextToken().trim());
            }
        }
        return result;
    }

}
