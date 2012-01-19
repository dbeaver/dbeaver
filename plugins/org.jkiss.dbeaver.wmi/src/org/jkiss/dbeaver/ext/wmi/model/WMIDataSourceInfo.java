/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.DBCStateType;
import org.jkiss.wmi.service.WMIService;

import java.util.Collection;
import java.util.Collections;

/**
 * Info
 */
public class WMIDataSourceInfo implements DBPDataSourceInfo {

    public WMIDataSourceInfo()
    {
    }

    public boolean isReadOnlyData()
    {
        return false;
    }

    public boolean isReadOnlyMetaData()
    {
        return false;
    }

    public String getDatabaseProductName()
    {
        return "WMI";
    }

    public String getDatabaseProductVersion()
    {
        return "?";
    }

    public String getDriverName()
    {
        return "JKISS WMI native driver";
    }

    public String getDriverVersion()
    {
        return "1.0";
    }

    public String getIdentifierQuoteString()
    {
        return "'";
    }

    public Collection<String> getSQLKeywords()
    {
        return Collections.emptyList();
    }

    public Collection<String> getNumericFunctions()
    {
        return Collections.emptyList();
    }

    public Collection<String> getStringFunctions()
    {
        return Collections.emptyList();
    }

    public Collection<String> getSystemFunctions()
    {
        return Collections.emptyList();
    }

    public Collection<String> getTimeDateFunctions()
    {
        return Collections.emptyList();
    }

    public Collection<String> getExecuteKeywords()
    {
        return Collections.emptyList();
    }

    public String getSearchStringEscape()
    {
        return "%";
    }

    public String getSchemaTerm()
    {
        return "Namespace";
    }

    public String getProcedureTerm()
    {
        return "Procedure";
    }

    public String getCatalogTerm()
    {
        return "Server";
    }

    public int getCatalogUsage()
    {
        return 0;
    }

    public int getSchemaUsage()
    {
        return 0;
    }

    public String getCatalogSeparator()
    {
        return ".";
    }

    public String getStructSeparator()
    {
        return ".";
    }

    public boolean isCatalogAtStart()
    {
        return true;
    }

    public DBCStateType getSQLStateType()
    {
        return DBCStateType.UNKNOWN;
    }

    public boolean supportsTransactions()
    {
        return false;
    }

    public boolean supportsSavepoints()
    {
        return false;
    }

    public boolean supportsReferentialIntegrity()
    {
        return false;
    }

    public boolean supportsIndexes()
    {
        return false;
    }

    public Collection<DBPTransactionIsolation> getSupportedTransactionIsolations()
    {
        return null;
    }

    public String getScriptDelimiter()
    {
        return ";";
    }

    public boolean validUnquotedCharacter(char c)
    {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_';
    }

    public boolean supportsUnquotedMixedCase()
    {
        return true;
    }

    public boolean supportsQuotedMixedCase()
    {
        return true;
    }

    public DBPIdentifierCase storesUnquotedCase()
    {
        return DBPIdentifierCase.MIXED;
    }

    public DBPIdentifierCase storesQuotedCase()
    {
        return DBPIdentifierCase.MIXED;
    }
}
