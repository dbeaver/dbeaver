/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.DBCStateType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * JDBCDataSourceInfo
 */
public class JDBCDataSourceInfo implements DBPDataSourceInfo
{
    static final Log log = LogFactory.getLog(JDBCDataSourceInfo.class);

    public static final String STRUCT_SEPARATOR = ".";
    public static final String TERM_SCHEMA = "Schema";
    public static final String TERM_PROCEDURE = "Procedure";
    public static final String TERM_CATALOG = "Database";

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
            this.databaseProductName = "?";
        }
        try {
            this.databaseProductVersion = metaData.getDatabaseProductVersion();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.databaseProductVersion = "?";
        }
        try {
            this.driverName = metaData.getDriverName();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.driverName = "?";
        }
        try {
            this.driverVersion = metaData.getDriverVersion();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.driverVersion = "?";
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
            this.sqlKeywords = Collections.emptyList();
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
            this.searchStringEscape = "\\";
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
            validCharacters = "";
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

    public boolean isReadOnlyData()
    {
        return readOnly;
    }

    public boolean isReadOnlyMetaData()
    {
        return readOnly;
    }

    public String getDatabaseProductName()
    {
        return databaseProductName;
    }

    public String getDatabaseProductVersion()
    {
        return databaseProductVersion;
    }

    public String getDriverName()
    {
        return driverName;
    }

    public String getDriverVersion()
    {
        return driverVersion;
    }

    public String getIdentifierQuoteString()
    {
        return identifierQuoteString;
    }

    public Collection<String> getSQLKeywords()
    {
        return sqlKeywords;
    }

    public Collection<String> getNumericFunctions()
    {
        return numericFunctions;
    }

    public Collection<String> getStringFunctions()
    {
        return stringFunctions;
    }

    public Collection<String> getSystemFunctions()
    {
        return systemFunctions;
    }

    public Collection<String> getTimeDateFunctions()
    {
        return timeDateFunctions;
    }

    public Collection<String> getExecuteKeywords()
    {
        return null;
    }

    public String getSearchStringEscape()
    {
        return searchStringEscape;
    }

    public String getSchemaTerm()
    {
        return schemaTerm;
    }

    public String getProcedureTerm()
    {
        return procedureTerm;
    }

    public String getCatalogTerm()
    {
        return catalogTerm;
    }

    public int getCatalogUsage()
    {
        return catalogUsage;
    }

    public int getSchemaUsage()
    {
        return schemaUsage;
    }

    public String getCatalogSeparator()
    {
        return catalogSeparator;
    }

    public String getStructSeparator()
    {
        return STRUCT_SEPARATOR;
    }

    public boolean isCatalogAtStart()
    {
        return isCatalogAtStart;
    }

    public DBCStateType getSQLStateType()
    {
        return sqlStateType;
    }

    public boolean supportsTransactions()
    {
        return supportsTransactions;
    }

    public boolean supportsSavepoints()
    {
        return false;
    }

    public boolean supportsReferentialIntegrity()
    {
        return supportsReferences;
    }

    public void setSupportsReferences(boolean supportsReferences)
    {
        this.supportsReferences = supportsReferences;
    }

    public boolean supportsIndexes()
    {
        return supportsIndexes;
    }

    public void setSupportsIndexes(boolean supportsIndexes)
    {
        this.supportsIndexes = supportsIndexes;
    }

    public Collection<DBPTransactionIsolation> getSupportedTransactionIsolations()
    {
        return supportedIsolations;
    }

    public String getScriptDelimiter()
    {
        return ";";
    }

    public boolean validUnquotedCharacter(char c)
    {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_' || validCharacters.indexOf(c) != -1;
    }

    public boolean supportsUnquotedMixedCase()
    {
        return supportsUnquotedMixedCase;
    }

    public boolean supportsQuotedMixedCase()
    {
        return supportsQuotedMixedCase;
    }

    public DBPIdentifierCase storesUnquotedCase()
    {
        return unquotedIdentCase;
    }

    public DBPIdentifierCase storesQuotedCase()
    {
        return quotedIdentCase;
    }

    private static List<String> makeStringList(String source)
    {
        List<String> result = new ArrayList<String>();
        if (source != null && source.length() > 0) {
            StringTokenizer st = new StringTokenizer(source, ";,");
            while (st.hasMoreTokens()) {
                result.add(st.nextToken().trim());
            }
        }
        return result;
    }
}
