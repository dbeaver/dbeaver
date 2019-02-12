/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.impl.BaseDataSourceInfo;
import org.osgi.framework.Version;

import java.util.Collection;

/**
 * Info
 */
public class WMIDataSourceInfo extends BaseDataSourceInfo {

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

}
