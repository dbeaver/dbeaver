package org.jkiss.dbeaver.model.sql.task;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;
import org.jkiss.dbeaver.model.struct.DBSObject;


public abstract class SQLToolTaskVersionValidator<SETTINGS extends SQLToolExecuteSettings<? extends DBSObject>, Object>
        implements IPropertyValueValidator<SETTINGS, Object> {

    @Override
    public boolean isValidValue(SETTINGS settings, Object value) throws IllegalArgumentException {
        if (!settings.getObjectList().isEmpty()) {
            DBPDataSource dataSource = settings.getObjectList().get(0).getDataSource();
            if (dataSource instanceof JDBCDataSource) {
                return ((JDBCDataSource) dataSource).isServerVersionAtLeast(getMajorVersion(), getMinorVersion());
            }
        }
        return false;
    }

    public abstract int getMajorVersion();

    public abstract int getMinorVersion();
}
