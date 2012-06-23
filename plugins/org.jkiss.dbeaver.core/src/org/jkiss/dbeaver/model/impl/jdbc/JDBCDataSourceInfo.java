/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.DBCStateType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * JDBCDataSourceInfo
 */
public class JDBCDataSourceInfo implements DBPDataSourceInfo
{
    static final Log log = LogFactory.getLog(JDBCDataSourceInfo.class);

    public static final String STRUCT_SEPARATOR = "."; //$NON-NLS-1$
    public static final String TERM_SCHEMA = CoreMessages.model_jdbc_Schema;
    public static final String TERM_PROCEDURE = CoreMessages.model_jdbc_Procedure;
    public static final String TERM_CATALOG = CoreMessages.model_jdbc_Database;

    private boolean readOnly;
    private String databaseProductName;
    private String databaseProductVersion;
    private String driverName;
    private String driverVersion;
    private String identifierQuoteString;
    private List<String> sqlKeywords;
    private List<String> numericFunctions;
    private List<String> stringFunctions;
    private List<String> systemFunctions;
    private List<String> timeDateFunctions;
    private String searchStringEscape;
    private String schemaTerm;
    private String procedureTerm;
    private String catalogTerm;
    private int catalogUsage;
    private int schemaUsage;
    private String catalogSeparator;
    private boolean isCatalogAtStart;
    private String validCharacters;

    private DBCStateType sqlStateType;
    private boolean supportsTransactions;
    private List<DBPTransactionIsolation> supportedIsolations;

    private boolean supportsUnquotedMixedCase;
    private boolean supportsQuotedMixedCase;
    private DBPIdentifierCase unquotedIdentCase;
    private DBPIdentifierCase quotedIdentCase;
    private boolean supportsReferences = true;
    private boolean supportsIndexes = true;

    public JDBCDataSourceInfo(DBSObject owner, JDBCDatabaseMetaData metaData)
    {
        try {
            this.readOnly = metaData.isReadOnly();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.readOnly = false;
        }
        try {
            this.databaseProductName = metaData.getDatabaseProductName();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.databaseProductName = "?"; //$NON-NLS-1$
        }
        try {
            this.databaseProductVersion = metaData.getDatabaseProductVersion();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.databaseProductVersion = "?"; //$NON-NLS-1$
        }
        try {
            this.driverName = metaData.getDriverName();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.driverName = "?"; //$NON-NLS-1$
        }
        try {
            this.driverVersion = metaData.getDriverVersion();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.driverVersion = "?"; //$NON-NLS-1$
        }
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
            this.schemaTerm = makeTermString(metaData.getSchemaTerm(), TERM_SCHEMA);
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.schemaTerm = TERM_SCHEMA;
        }
        try {
            this.procedureTerm = makeTermString(metaData.getProcedureTerm(), TERM_PROCEDURE);
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.procedureTerm = TERM_PROCEDURE;
        }
        try {
            this.catalogTerm = makeTermString(metaData.getCatalogTerm(), TERM_CATALOG);
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.catalogTerm = TERM_CATALOG;
        }
        try {
            this.catalogSeparator = metaData.getCatalogSeparator();
            if (CommonUtils.isEmpty(this.catalogSeparator)) {
                this.catalogSeparator = STRUCT_SEPARATOR;
            }
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.catalogSeparator = STRUCT_SEPARATOR;
        }
        try {
            catalogUsage = 
                (metaData.supportsCatalogsInDataManipulation() ? USAGE_DML : 0) |
                (metaData.supportsCatalogsInTableDefinitions() ? USAGE_DDL : 0) |
                (metaData.supportsCatalogsInProcedureCalls() ? USAGE_PROC : 0) |
                (metaData.supportsCatalogsInIndexDefinitions() ? USAGE_INDEX : 0) |
                (metaData.supportsCatalogsInPrivilegeDefinitions() ? USAGE_PRIV : 0);
        } catch (SQLException e) {
            log.debug(e.getMessage());
            catalogUsage = USAGE_NONE;
        }
        try {
            schemaUsage = 
                (metaData.supportsSchemasInDataManipulation() ? USAGE_DML : 0) |
                (metaData.supportsSchemasInTableDefinitions() ? USAGE_DDL : 0) |
                (metaData.supportsSchemasInProcedureCalls() ? USAGE_PROC : 0) |
                (metaData.supportsSchemasInIndexDefinitions() ? USAGE_INDEX : 0) |
                (metaData.supportsSchemasInPrivilegeDefinitions() ? USAGE_PRIV : 0);
        } catch (SQLException e) {
            log.debug(e.getMessage());
            schemaUsage = USAGE_NONE;
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
        try {
            switch (metaData.getSQLStateType()) {
                case DatabaseMetaData.sqlStateXOpen:
                    this.sqlStateType = DBCStateType.XOPEN;
                    break;
                case DatabaseMetaData.sqlStateSQL99:
                    this.sqlStateType = DBCStateType.SQL99;
                    break;
                default:
                    this.sqlStateType = DBCStateType.UNKNOWN;
                    break;
            }
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.sqlStateType = DBCStateType.UNKNOWN;
        }

        try {
            supportsTransactions = metaData.supportsTransactions();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            supportsTransactions = true;
        }

        supportedIsolations = new ArrayList<DBPTransactionIsolation>();
        try {
            for (JDBCTransactionIsolation txi : JDBCTransactionIsolation.values()) {
                if (metaData.supportsTransactionIsolationLevel(txi.getCode())) {
                    supportedIsolations.add(txi);
                }
            }
        } catch (Throwable e) {
            log.debug(e.getMessage());
            supportsTransactions = true;
        }
        if (!supportedIsolations.contains(JDBCTransactionIsolation.NONE)) {
            supportedIsolations.add(0, JDBCTransactionIsolation.NONE);
        }
    }

    private String makeTermString(String term, String defTerm)
    {
        return CommonUtils.isEmpty(term) ? defTerm : CommonUtils.capitalizeWord(term.toLowerCase());
    }

    @Override
    public boolean isReadOnlyData()
    {
        return readOnly;
    }

    @Override
    public boolean isReadOnlyMetaData()
    {
        return readOnly;
    }

    @Override
    public String getDatabaseProductName()
    {
        return databaseProductName;
    }

    @Override
    public String getDatabaseProductVersion()
    {
        return databaseProductVersion;
    }

    @Override
    public String getDriverName()
    {
        return driverName;
    }

    @Override
    public String getDriverVersion()
    {
        return driverVersion;
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
    public String getSearchStringEscape()
    {
        return searchStringEscape;
    }

    @Override
    public String getSchemaTerm()
    {
        return schemaTerm;
    }

    @Override
    public String getProcedureTerm()
    {
        return procedureTerm;
    }

    @Override
    public String getCatalogTerm()
    {
        return catalogTerm;
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
    public String getStructSeparator()
    {
        return STRUCT_SEPARATOR;
    }

    @Override
    public boolean isCatalogAtStart()
    {
        return isCatalogAtStart;
    }

    @Override
    public DBCStateType getSQLStateType()
    {
        return sqlStateType;
    }

    @Override
    public boolean supportsTransactions()
    {
        return supportsTransactions;
    }

    @Override
    public boolean supportsSavepoints()
    {
        return false;
    }

    @Override
    public boolean supportsReferentialIntegrity()
    {
        return supportsReferences;
    }

    public void setSupportsReferences(boolean supportsReferences)
    {
        this.supportsReferences = supportsReferences;
    }

    @Override
    public boolean supportsIndexes()
    {
        return supportsIndexes;
    }

    public void setSupportsIndexes(boolean supportsIndexes)
    {
        this.supportsIndexes = supportsIndexes;
    }

    @Override
    public Collection<DBPTransactionIsolation> getSupportedTransactionIsolations()
    {
        return supportedIsolations;
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
