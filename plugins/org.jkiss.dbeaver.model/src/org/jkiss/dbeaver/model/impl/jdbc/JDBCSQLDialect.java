/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLStateType;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * SQL Dialect JDBC API implementation
 */
public class JDBCSQLDialect extends BasicSQLDialect {

    private static final Log log = Log.getLog(JDBCSQLDialect.class);

    private String name;
    private String[][] identifierQuoteString;
    private SQLStateType sqlStateType;
    private String searchStringEscape;
    private String catalogSeparator;
    private boolean isCatalogAtStart;
    private int catalogUsage;
    protected int schemaUsage;
    private String validCharacters;
    private boolean supportsUnquotedMixedCase;
    private boolean supportsQuotedMixedCase;
    private DBPIdentifierCase unquotedIdentCase;
    private DBPIdentifierCase quotedIdentCase;
    private boolean supportsSubqueries = false;

    private transient boolean typesLoaded = false;

    public JDBCSQLDialect(String name) {
        this.name = name;
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        String singleQuoteStr;
        try {
            singleQuoteStr = metaData.getIdentifierQuoteString();
        } catch (Throwable e) {
            log.debug("Error getting identifierQuoteString: " + e.getMessage());
            singleQuoteStr = SQLConstants.DEFAULT_IDENTIFIER_QUOTE;
        }
        if (singleQuoteStr != null) {
            singleQuoteStr = singleQuoteStr.trim();
            if (singleQuoteStr.isEmpty()) {
                singleQuoteStr = null;
            }
        }
        if (singleQuoteStr == null) {
            identifierQuoteString = new String[0][];
        } else {
            identifierQuoteString = new String[][] { { singleQuoteStr, singleQuoteStr } };
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
            log.debug("Error getting sqlStateType: " + e.getMessage());
            this.sqlStateType = SQLStateType.UNKNOWN;
        }

        try {
            supportsSubqueries = metaData.supportsCorrelatedSubqueries();
        } catch (SQLException e) {
            log.debug("Error getting supportsSubqueries: " + e.getMessage());
        }

        try {
            this.supportsUnquotedMixedCase = metaData.supportsMixedCaseIdentifiers();
        } catch (SQLException e) {
            log.debug("Error getting supportsUnquotedMixedCase:" + e.getMessage());
            this.supportsUnquotedMixedCase = false;
        }
        try {
            this.supportsQuotedMixedCase = metaData.supportsMixedCaseQuotedIdentifiers();
        } catch (SQLException e) {
            log.debug("Error getting supportsQuotedMixedCase: " + e.getMessage());
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
            log.debug("Error getting unquotedIdentCase:" + e.getMessage());
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
            log.debug("Error getting quotedIdentCase:" + e.getMessage());
            this.quotedIdentCase = DBPIdentifierCase.MIXED;
        }
        try {
            this.searchStringEscape = metaData.getSearchStringEscape();
        } catch (Throwable e) {
            log.debug("Error getting searchStringEscape:" + e.getMessage());
        }
        if (this.searchStringEscape == null) {
            this.searchStringEscape = ""; //$NON-NLS-1$
        }
        try {
            this.catalogSeparator = metaData.getCatalogSeparator();
            if (CommonUtils.isEmpty(this.catalogSeparator)) {
                this.catalogSeparator = String.valueOf(SQLConstants.STRUCT_SEPARATOR);
            }
        } catch (Throwable e) {
            log.debug("Error getting catalogSeparator:" + e.getMessage());
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
            log.debug("Error getting catalogUsage:" + e.getMessage());
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
            log.debug("Error getting schemaUsage:" + e.getMessage());
            schemaUsage = SQLDialect.USAGE_DDL | SQLDialect.USAGE_DML;
        }
        try {
            validCharacters = metaData.getExtraNameCharacters();
        } catch (SQLException e) {
            log.debug("Error getting validCharacters:" + e.getMessage());
            validCharacters = ""; //$NON-NLS-1$
        }

        try {
            this.isCatalogAtStart = metaData.isCatalogAtStart();
        } catch (Throwable e) {
            log.debug("Error getting isCatalogAtStart:" + e.getMessage());
            this.isCatalogAtStart = true;
        }

        loadDriverKeywords(metaData);
    }

    @NotNull
    @Override
    public String getDialectName() {
        return name;
    }

    @Nullable
    @Override
    public String[][] getIdentifierQuoteStrings()
    {
        return identifierQuoteString;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return new String[0];
    }

    @NotNull
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

    @NotNull
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

    @NotNull
    @Override
    public SQLStateType getSQLStateType()
    {
        return sqlStateType;
    }

    @NotNull
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

    @NotNull
    @Override
    public DBPIdentifierCase storesUnquotedCase()
    {
        return unquotedIdentCase;
    }

    @NotNull
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

    public boolean supportsUpsertStatement() {
        return false;
    }

    @NotNull
    @Override
    public TreeSet<String> getDataTypes(@NotNull DBPDataSource dataSource) {
        if (!typesLoaded && dataSource instanceof JDBCDataSource) {
            types.clear();
            loadDataTypesFromDatabase((JDBCDataSource) dataSource);
            typesLoaded = true;
        }
        return types;
    }

    protected void loadDataTypesFromDatabase(JDBCDataSource dataSource) {
        Collection<? extends DBSDataType> supportedDataTypes = dataSource.getLocalDataTypes();
        if (supportedDataTypes != null) {
            for (DBSDataType dataType : supportedDataTypes) {
                if (!dataType.getDataKind().isComplex()) {
                    types.add(dataType.getName().toUpperCase(Locale.ENGLISH));
                }
            }
        }

        if (types.isEmpty()) {
            // Add default types
            Collections.addAll(types, SQLConstants.DEFAULT_TYPES);
        }
        addKeywords(types, DBPKeywordType.TYPE);
    }

    private void loadDriverKeywords(JDBCDatabaseMetaData metaData)
    {
        try {
            // Keywords
            Collection<String> sqlKeywords = makeStringList(metaData.getSQLKeywords());
            if (!CommonUtils.isEmpty(sqlKeywords)) {
                for (String keyword : sqlKeywords) {
                    addSQLKeyword(keyword.toUpperCase());
                }
            }
        } catch (SQLException e) {
            log.debug("Error reading SQL keywords: " + e.getMessage());
        }
        try {
            // Functions
            Set<String> allFunctions = new HashSet<>();
            for (String func : makeStringList(metaData.getNumericFunctions())) {
                allFunctions.add(func.toUpperCase());
            }
            for (String func : makeStringList(metaData.getStringFunctions())) {
                allFunctions.add(func.toUpperCase());
            }
            for (String func : makeStringList(metaData.getSystemFunctions())) {
                allFunctions.add(func.toUpperCase());
            }
            for (String func : makeStringList(metaData.getTimeDateFunctions())) {
                allFunctions.add(func.toUpperCase());
            }
            // Remove functions which clashes with keywords
            for (Iterator<String> fIter = allFunctions.iterator(); fIter.hasNext(); ) {
                if (getKeywordType(fIter.next())== DBPKeywordType.KEYWORD) {
                    fIter.remove();
                }
            }
            addFunctions(allFunctions);
        }
        catch (Throwable e) {
            log.debug("Error reading SQL functions: " + e.getMessage());
        }
    }

    private static List<String> makeStringList(String source)
    {
        List<String> result = new ArrayList<>();
        if (source != null && source.length() > 0) {
            StringTokenizer st = new StringTokenizer(source, ";,"); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                result.add(st.nextToken().trim());
            }
        }
        return result;
    }

}
