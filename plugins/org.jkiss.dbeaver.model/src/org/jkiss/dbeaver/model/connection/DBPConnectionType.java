package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourcePermission;
import org.jkiss.dbeaver.model.DBPDataSourcePermissionOwner;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Connection type
 */
public class DBPConnectionType implements DBPDataSourcePermissionOwner {

    public static final DBPConnectionType DEV = new DBPConnectionType("dev", ModelMessages.dbp_connection_type_table_development, "255,255,255", ModelMessages.dbp_connection_type_table_regular_development_database, true, false, false, true, null); //$NON-NLS-1$ //$NON-NLS-3$
    public static final DBPConnectionType TEST = new DBPConnectionType("test", ModelMessages.dbp_connection_type_table_test, "org.jkiss.dbeaver.color.connectionType.qa.background", ModelMessages.dbp_connection_type_table_test_database, true, false, true, true, null); //$NON-NLS-1$ //$NON-NLS-3$
    public static final DBPConnectionType PROD = new DBPConnectionType("prod", ModelMessages.dbp_connection_type_table_production, "org.jkiss.dbeaver.color.connectionType.prod.background", ModelMessages.dbp_connection_type_table_production_database, false, true, true, true, null); //$NON-NLS-1$ //$NON-NLS-3$

    public static final DBPConnectionType[] SYSTEM_TYPES = {DEV, TEST, PROD};
    public static final DBPConnectionType DEFAULT_TYPE = DEV;

    private String id;
    private String name;
    private String color;
    private String description;
    private boolean autocommit;
    private boolean confirmExecute;
    private boolean confirmDataChange;
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
        boolean confirmDataChange)
    {
        this(id, name, color, description, autocommit, confirmExecute, confirmDataChange, false, null);
    }

    private DBPConnectionType(
        String id,
        String name,
        String color,
        String description,
        boolean autocommit,
        boolean confirmExecute,
        boolean confirmDataChange,
        boolean predefined,
        List<DBPDataSourcePermission> connectionModifyRestrictions)
    {
        this.id = id;
        this.name = name;
        this.color = color;
        this.description = description;
        this.autocommit = autocommit;
        this.confirmExecute = confirmExecute;
        this.confirmDataChange = confirmDataChange;
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
        this.color = color;
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

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DBPConnectionType) {
            DBPConnectionType ct = (DBPConnectionType)obj;
            return CommonUtils.equalObjects(id, ct.id) &&
                CommonUtils.equalObjects(name, ct.name) &&
                CommonUtils.equalObjects(color, ct.color) &&
                CommonUtils.equalObjects(description, ct.description) &&
                autocommit == ct.autocommit &&
                confirmExecute == ct.confirmExecute &&
                confirmDataChange == ct.confirmDataChange &&
                predefined == ct.predefined &&
                CommonUtils.equalObjects(connectionModifyRestrictions, ct.connectionModifyRestrictions);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
