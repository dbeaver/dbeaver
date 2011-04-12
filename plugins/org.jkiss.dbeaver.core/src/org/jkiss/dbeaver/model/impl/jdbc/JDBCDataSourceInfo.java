/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.DBCStateType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.struct.DBSDataType;

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
    private DBCStateType sqlStateType;
    private boolean supportsTransactions;
    private List<DBPTransactionIsolation> supportedIsolations;

    private List<DBSDataType> dataTypeList;
    private Map<String, DBSDataType> dataTypeMap;

    public JDBCDataSourceInfo(JDBCDatabaseMetaData metaData)
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
            this.identifierQuoteString = "\"";
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
            this.schemaTerm = metaData.getSchemaTerm();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.schemaTerm = "Schema";
        }
        try {
            this.procedureTerm = metaData.getProcedureTerm();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.procedureTerm = "Procedure";
        }
        try {
            this.catalogTerm = metaData.getCatalogTerm();
        } catch (Throwable e) {
            log.debug(e.getMessage());
            this.catalogTerm = "Database";
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

        // Extract datatypes
        this.dataTypeList = new ArrayList<DBSDataType>();
        this.dataTypeMap = new HashMap<String, DBSDataType>();
        try {
            // Read data types
            JDBCResultSet dbResult = metaData.getTypeInfo();
            try {
                while (dbResult.next()) {
                    String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.LOCAL_TYPE_NAME);
                    int type = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
                    int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
                    int minScale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.MINIMUM_SCALE);
                    int maxScale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.MAXIMUM_SCALE);
                    boolean isSearchable = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SEARCHABLE) != 0;
                    boolean isUnsigned = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.UNSIGNED_ATTRIBUTE);
                    JDBCDataType dataType = new JDBCDataType(
                        type,
                        typeName,
                        remarks,
                        isUnsigned,
                        isSearchable,
                        precision,
                        minScale,
                        maxScale);
                    addDataType(dataType);
                }
            } finally {
                dbResult.close();
            }
        }
        catch (SQLException ex) {
            // Error getting datatypes list
            log.debug(ex.getMessage());
        }
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

    public List<String> getSQLKeywords()
    {
        return sqlKeywords;
    }

    public List<String> getNumericFunctions()
    {
        return numericFunctions;
    }

    public List<String> getStringFunctions()
    {
        return stringFunctions;
    }

    public List<String> getSystemFunctions()
    {
        return systemFunctions;
    }

    public List<String> getTimeDateFunctions()
    {
        return timeDateFunctions;
    }

    public List<String> getExecuteKeywords()
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

    public List<DBPTransactionIsolation> getSupportedTransactionIsolations()
    {
        return supportedIsolations;
    }

    public List<DBSDataType> getSupportedDataTypes()
    {
        return dataTypeList;
    }

    public DBSDataType getSupportedDataType(String typeName)
    {
        return dataTypeMap.get(typeName);
    }

    public String getScriptDelimiter()
    {
        return ";";
    }

    private void addDataType(JDBCDataType dataType)
    {
        dataTypeList.add(dataType);
        dataTypeMap.put(dataType.getName(), dataType);
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
