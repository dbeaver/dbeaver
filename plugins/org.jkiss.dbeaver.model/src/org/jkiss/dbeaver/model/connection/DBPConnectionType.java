/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourcePermission;
import org.jkiss.dbeaver.model.DBPDataSourcePermissionOwner;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Connection type
 */
public class DBPConnectionType implements DBPDataSourcePermissionOwner {

    public static final DBPConnectionType DEV;
    public static final DBPConnectionType TEST;
    public static final DBPConnectionType PROD;

    public static final DBPConnectionType[] SYSTEM_TYPES;
    public static final DBPConnectionType DEFAULT_TYPE;

    static {
        DEV = new DBPConnectionType(
            "dev",
            ModelMessages.dbp_connection_type_table_development,
            "255,255,255",
            ModelMessages.dbp_connection_type_table_regular_development_database,
            true,
            false,
            false,
            false,
            true,
            true,
            1800, //30 minutes
            true,
            14400, //1 hour
            true,
            null); //$NON-NLS-1$ //$NON-NLS-3$
        TEST = new DBPConnectionType(
            "test",
            ModelMessages.dbp_connection_type_table_test,
            "org.jkiss.dbeaver.color.connectionType.qa.background",
            ModelMessages.dbp_connection_type_table_test_database,
            true,
            false,
            true,
            false,
            true,
            true,
            900, //30 minutes
            true,
            7200, //2 hours
            true,
            null); //$NON-NLS-1$ //$NON-NLS-3$
        PROD = new DBPConnectionType(
            "prod",
            ModelMessages.dbp_connection_type_table_production,
            "org.jkiss.dbeaver.color.connectionType.prod.background",
            ModelMessages.dbp_connection_type_table_production_database,
            false,
            true,
            true,
            false,
            false,
            true,
            600, //10 minutes
            true,
            3600, //1 hour
            true,
            null); //$NON-NLS-1$ //$NON-NLS-3$

        SYSTEM_TYPES = new DBPConnectionType[] { DEV, TEST, PROD };
        DEFAULT_TYPE = DEV;
    }

    private String id;
    private String name;
    private String color;
    private String description;
    private boolean autocommit;
    private boolean confirmExecute;
    private boolean confirmDataChange;
    private boolean smartCommit;
    private boolean smartCommitRecover;
    private boolean autoCloseTransactions;
    private int closeIdleTransactionPeriod;
    private boolean autoCloseConnections;
    private int closeIdleConnectionPeriod;

    private final boolean predefined;
    private List<DBPDataSourcePermission> connectionModifyRestrictions;

    public DBPConnectionType(DBPConnectionType source) {
        this(
            source.id,
            source.name,
            source.color,
            source.description,
            source.autocommit,
            source.confirmExecute,
            source.confirmDataChange,
            source.smartCommit,
            source.smartCommitRecover,
            source.autoCloseTransactions,
            source.closeIdleTransactionPeriod,
            source.autoCloseConnections,
            source.closeIdleConnectionPeriod,
            source.predefined,
            source.connectionModifyRestrictions);
    }

    public DBPConnectionType(
        String id,
        String name,
        String color,
        String description,
        boolean autocommit,
        boolean confirmExecute,
        boolean confirmDataChange,
        boolean smartCommit,
        boolean smartCommitRecover,
        boolean autoCloseTransactions,
        int closeIdleTransactionPeriod,
        boolean autoCloseConnections,
        int closeIdleConnectionPeriod)
    {
        this(
            id,
            name,
            color,
            description,
            autocommit,
            confirmExecute,
            confirmDataChange,
            smartCommit,
            smartCommitRecover,
            autoCloseTransactions,
            closeIdleTransactionPeriod,
            autoCloseConnections,
            closeIdleConnectionPeriod,
            false,
            null);
    }

    private DBPConnectionType(
        String id,
        String name,
        String color,
        String description,
        boolean autocommit,
        boolean confirmExecute,
        boolean confirmDataChange,
        boolean smartCommit,
        boolean smartCommitRecover,
        boolean autoCloseTransactions,
        int closeIdleTransactionPeriod,
        boolean autoCloseConnections,
        int closeIdleConnectionPeriod,
        boolean predefined,
        List<DBPDataSourcePermission> connectionModifyRestrictions)
    {
        this.id = id;
        this.name = name;
        this.color = getColorValueFixed(color);
        this.description = description;
        this.autocommit = autocommit;
        this.confirmExecute = confirmExecute;
        this.confirmDataChange = confirmDataChange;
        this.smartCommit = smartCommit;
        this.smartCommitRecover = smartCommitRecover;
        this.autoCloseTransactions = autoCloseTransactions;
        this.closeIdleTransactionPeriod = closeIdleTransactionPeriod;
        this.autoCloseConnections = autoCloseConnections;
        this.closeIdleConnectionPeriod = closeIdleConnectionPeriod;
        this.predefined = predefined;
        if (connectionModifyRestrictions != null) {
            this.connectionModifyRestrictions = new ArrayList<>(connectionModifyRestrictions);
        }
    }

    public boolean isPredefined() {
        return predefined;
    }

    public String getId() {
        return id;
    }

    public String setId(String id) {
        return this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = getColorValueFixed(color);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAutocommit() {
        return autocommit;
    }

    public void setAutocommit(boolean autocommit) {
        this.autocommit = autocommit;
    }

    public boolean isConfirmExecute() {
        return confirmExecute;
    }

    public void setConfirmExecute(boolean confirmExecute) {
        this.confirmExecute = confirmExecute;
    }

    public boolean isConfirmDataChange() {
        return confirmDataChange;
    }

    public void setConfirmDataChange(boolean confirmDataChange) {
        this.confirmDataChange = confirmDataChange;
    }

    public boolean isSmartCommit() {
        return smartCommit;
    }

    public void setSmartCommit(boolean smartCommit) {
        this.smartCommit = smartCommit;
    }

    public boolean isSmartCommitRecover() {
        return smartCommitRecover;
    }

    public void setSmartCommitRecover(boolean smartCommitRecover) {
        this.smartCommitRecover = smartCommitRecover;
    }

    public boolean isAutoCloseTransactions() {
        return autoCloseTransactions;
    }

    public void setAutoCloseTransactions(boolean autoCloseTransactions) {
        this.autoCloseTransactions = autoCloseTransactions;
    }

    public int getCloseIdleTransactionPeriod() {
        return closeIdleTransactionPeriod;
    }

    public void setCloseIdleTransactionPeriod(int closeIdleTransactionPeriod) {
        this.closeIdleTransactionPeriod = closeIdleTransactionPeriod;
    }

    public boolean isAutoCloseConnections() {
        return autoCloseConnections;
    }

    public void setAutoCloseConnections(boolean autoCloseConnections) {
        this.autoCloseConnections = autoCloseConnections;
    }
    
    public int getCloseIdleConnectionPeriod() {
        return closeIdleConnectionPeriod;
    }

    public void setCloseIdleConnectionPeriod(int closeIdleConnectionPeriod) {
        this.closeIdleConnectionPeriod = closeIdleConnectionPeriod;
    }

    @Override
    public boolean hasModifyPermission(DBPDataSourcePermission permission) {
        return connectionModifyRestrictions == null || !connectionModifyRestrictions.contains(permission);
    }

    @Override
    public List<DBPDataSourcePermission> getModifyPermission() {
        if (CommonUtils.isEmpty(this.connectionModifyRestrictions)) {
            return Collections.emptyList();
        } else {
            return new ArrayList<>(this.connectionModifyRestrictions);
        }
    }

    @Override
    public void setModifyPermissions(@Nullable Collection<DBPDataSourcePermission> permissions) {
        if (CommonUtils.isEmpty(permissions)) {
            this.connectionModifyRestrictions = null;
        } else {
            this.connectionModifyRestrictions = new ArrayList<>(permissions);
        }
    }

    private String getColorValueFixed(String color) {
        // Backward compatibility.
        // In old times we had hardcoded colors now we need to change them to color constants
        if (PROD != null && this.id.equals(PROD.id) && color.equals("247,159,129")) {
            return PROD.color;
        } else if (TEST != null && this.id.equals(TEST.id) && color.equals("196,255,181")) {
            return TEST.color;
        }
        return color;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DBPConnectionType ct) {
            return CommonUtils.equalObjects(id, ct.id) &&
                CommonUtils.equalObjects(name, ct.name) &&
                CommonUtils.equalObjects(color, ct.color) &&
                CommonUtils.equalObjects(description, ct.description) &&
                autocommit == ct.autocommit &&
                confirmExecute == ct.confirmExecute &&
                confirmDataChange == ct.confirmDataChange &&
                smartCommit == ct.smartCommit &&
                smartCommitRecover == ct.smartCommitRecover &&
                autoCloseTransactions == ct.autoCloseTransactions &&
                CommonUtils.equalObjects(closeIdleTransactionPeriod, ct.closeIdleTransactionPeriod) &&
                autoCloseConnections == ct.autoCloseConnections &&
                CommonUtils.equalObjects(closeIdleConnectionPeriod, ct.closeIdleConnectionPeriod) &&
                predefined == ct.predefined &&
                CommonUtils.equalObjects(connectionModifyRestrictions, ct.connectionModifyRestrictions);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private static final String DEFAULT_CONNECTION_TYPE_PREF = "default.connection.type";

    public static DBPConnectionType getDefaultConnectionType() {
        String defTypeName = DBWorkbench.getPlatform().getPreferenceStore().getString(DEFAULT_CONNECTION_TYPE_PREF);
        if (CommonUtils.isEmpty(defTypeName)) {
            defTypeName = DEV.getId();
        }

        return DBWorkbench.getPlatform().getDataSourceProviderRegistry().getConnectionType(defTypeName, DEV);
    }

    public static void setDefaultConnectionType(DBPConnectionType connectionType) {
        DBWorkbench.getPlatform().getPreferenceStore().setValue(DEFAULT_CONNECTION_TYPE_PREF, connectionType.getId());
    }

}
