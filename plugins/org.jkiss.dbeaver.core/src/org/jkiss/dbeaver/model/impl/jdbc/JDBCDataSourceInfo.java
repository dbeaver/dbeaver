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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JDBCDataSourceInfo
 */
public class JDBCDataSourceInfo implements DBPDataSourceInfo
{
    static final Log log = Log.getLog(JDBCDataSourceInfo.class);

    public static final String TERM_SCHEMA = CoreMessages.model_jdbc_Schema;
    public static final String TERM_PROCEDURE = CoreMessages.model_jdbc_Procedure;
    public static final String TERM_CATALOG = CoreMessages.model_jdbc_Database;

    private boolean readOnly;
    private String databaseProductName;
    private String databaseProductVersion;
    private String driverName;
    private String driverVersion;
    private Version databaseVersion;
    private String schemaTerm;
    private String procedureTerm;
    private String catalogTerm;

    private boolean supportsTransactions;
    private List<DBPTransactionIsolation> supportedIsolations;

    private boolean supportsReferences = true;
    private boolean supportsIndexes = true;
    private boolean supportsStoredCode = true;
    private boolean supportsBatchUpdates = false;
    private boolean supportsScroll;

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
            databaseVersion = new Version(metaData.getDatabaseMajorVersion(), metaData.getDatabaseMinorVersion(), 0);
        } catch (Throwable e) {
            try {
                databaseVersion = new Version(databaseProductVersion);
            } catch (IllegalArgumentException e1) {
                log.debug("Can't determine database version. Use default");
                databaseVersion = new Version(0, 0, 0);
            }
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
            supportsBatchUpdates = metaData.supportsBatchUpdates();
        } catch (Throwable e) {
            log.debug(e);
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

        supportsScroll = true;
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
    public Version getDatabaseVersion() {
        return databaseVersion;
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
    public boolean supportsStoredCode() {
        return supportsStoredCode;
    }

    public void setSupportsStoredCode(boolean supportsStoredCode) {
        this.supportsStoredCode = supportsStoredCode;
    }

    @Override
    public Collection<DBPTransactionIsolation> getSupportedTransactionsIsolation()
    {
        return supportedIsolations;
    }

    @Override
    public boolean supportsResultSetLimit() {
        return true;
    }

    @Override
    public boolean supportsResultSetScroll()
    {
        return supportsScroll;
    }

    @Override
    public boolean isDynamicMetadata() {
        return false;
    }

    @Override
    public boolean supportsMultipleResults() {
        return false;
    }

    public void setSupportsResultSetScroll(boolean supportsScroll)
    {
        this.supportsScroll = supportsScroll;
    }

    @Override
    public boolean supportsBatchUpdates()
    {
        return supportsBatchUpdates;
    }

}
