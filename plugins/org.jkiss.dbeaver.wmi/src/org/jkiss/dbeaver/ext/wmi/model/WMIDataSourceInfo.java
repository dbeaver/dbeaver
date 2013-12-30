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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.DBCStateType;

import java.util.Collection;
import java.util.Collections;

/**
 * Info
 */
public class WMIDataSourceInfo implements DBPDataSourceInfo {

    public WMIDataSourceInfo()
    {
    }

    @Override
    public boolean isReadOnlyData()
    {
        return false;
    }

    @Override
    public boolean isReadOnlyMetaData()
    {
        return false;
    }

    @Override
    public String getDatabaseProductName()
    {
        return "WMI";
    }

    @Override
    public String getDatabaseProductVersion()
    {
        return "?";
    }

    @Override
    public String getDriverName()
    {
        return "JKISS WMI native driver";
    }

    @Override
    public String getDriverVersion()
    {
        return "1.0";
    }

    @Override
    public String getIdentifierQuoteString()
    {
        return "'";
    }

    @Override
    public Collection<String> getSQLKeywords()
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getNumericFunctions()
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getStringFunctions()
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getSystemFunctions()
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getTimeDateFunctions()
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getExecuteKeywords()
    {
        return Collections.emptyList();
    }

    @Override
    public String getSearchStringEscape()
    {
        return "%";
    }

    @Override
    public String getSchemaTerm()
    {
        return "Namespace";
    }

    @Override
    public String getProcedureTerm()
    {
        return "Procedure";
    }

    @Override
    public String getCatalogTerm()
    {
        return "Server";
    }

    @Override
    public int getCatalogUsage()
    {
        return 0;
    }

    @Override
    public int getSchemaUsage()
    {
        return 0;
    }

    @Override
    public String getCatalogSeparator()
    {
        return ".";
    }

    @Override
    public char getStructSeparator()
    {
        return '.';
    }

    @Override
    public boolean isCatalogAtStart()
    {
        return true;
    }

    @Override
    public DBCStateType getSQLStateType()
    {
        return DBCStateType.UNKNOWN;
    }

    @Override
    public boolean supportsTransactions()
    {
        return false;
    }

    @Override
    public boolean supportsSavepoints()
    {
        return false;
    }

    @Override
    public boolean supportsReferentialIntegrity()
    {
        return false;
    }

    @Override
    public boolean supportsIndexes()
    {
        return false;
    }

    @Override
    public Collection<DBPTransactionIsolation> getSupportedTransactionsIsolation()
    {
        return null;
    }

    @Override
    public String getScriptDelimiter()
    {
        return ";";
    }

    @Override
    public boolean validUnquotedCharacter(char c)
    {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_';
    }

    @Override
    public boolean supportsUnquotedMixedCase()
    {
        return true;
    }

    @Override
    public boolean supportsQuotedMixedCase()
    {
        return true;
    }

    @Override
    public boolean supportsSubqueries()
    {
        return false;
    }

    @Override
    public boolean supportsBatchUpdates()
    {
        return false;
    }

    @Override
    public DBPIdentifierCase storesUnquotedCase()
    {
        return DBPIdentifierCase.MIXED;
    }

    @Override
    public DBPIdentifierCase storesQuotedCase()
    {
        return DBPIdentifierCase.MIXED;
    }

    @Override
    public boolean supportsResultSetLimit() {
        return true;
    }

    @Override
    public boolean supportsResultSetScroll() {
        return false;
    }
}
