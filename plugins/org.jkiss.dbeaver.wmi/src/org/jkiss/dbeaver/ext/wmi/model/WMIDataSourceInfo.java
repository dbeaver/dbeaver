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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.osgi.framework.Version;

import java.util.Collection;

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
    public Version getDatabaseVersion() {
        return new Version(1, 0, 0);
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
    public boolean supportsStoredCode() {
        return false;
    }

    @Override
    public Collection<DBPTransactionIsolation> getSupportedTransactionsIsolation()
    {
        return null;
    }

    @Override
    public boolean supportsBatchUpdates()
    {
        return false;
    }

    @Override
    public boolean supportsResultSetLimit() {
        return true;
    }

    @Override
    public boolean supportsResultSetScroll() {
        return false;
    }

    @Override
    public boolean isDynamicMetadata() {
        return false;
    }

    @Override
    public boolean supportsMultipleResults() {
        return false;
    }
}
