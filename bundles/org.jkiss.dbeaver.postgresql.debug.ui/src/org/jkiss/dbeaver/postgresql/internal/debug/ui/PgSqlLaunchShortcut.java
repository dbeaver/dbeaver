package org.jkiss.dbeaver.postgresql.internal.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.debug.ui.LaunchShortcut;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.postgresql.debug.core.PostgreSqlDebugCore;

public class PgSqlLaunchShortcut extends LaunchShortcut {

    @Override
    protected String getSelectionEmptyMessage()
    {
        return "Selection does not containt PostgreSQL procedure";
    }

    @Override
    protected String getEditorEmptyMessage()
    {
        return "Editor does not containt PostgreSQL procedure";
    }

    @Override
    protected String getLaunchableSelectionTitle(String mode)
    {
        return "Select PostgreSQL Procedure";
    }

    @Override
    protected String getLaunchableSelectionMessage(String mode)
    {
        return "Select &PostgreSQL Procedure (? = any character, * = any String):";
    }

    @Override
    protected String getConfigurationTypeId()
    {
        return PostgreSqlDebugCore.CONFIGURATION_TYPE;
    }

    @Override
    protected boolean isCandidate(ILaunchConfiguration config, DBSObject launchable)
    {
        if (!config.exists()) {
            return false;
        }
        boolean isInstance = launchable instanceof PostgreProcedure;
        if (!isInstance) {
            return false;
        }
        PostgreProcedure procedure = (PostgreProcedure) launchable;

        String datasource = DebugCore.extractDatasource(config);
        String id = launchable.getDataSource().getContainer().getId();
        if (!datasource.equals(id)) {
            return false;
        }

        String database = DebugCore.extractDatabase(config);
        String databaseName = launchable.getDataSource().getName();
        if (!database.equals(databaseName)) {
            return false;
        }

        try {
            String oid = config.getAttribute(PostgreSqlDebugCore.ATTR_OID, String.valueOf(0));
            long objectId = procedure.getObjectId();
            if (!(Long.parseLong(oid)==objectId)) {
                return false;
            }
        } catch (Exception e) {
            //ignore
            return false;
        }
        return true;
    }

    @Override
    protected ILaunchConfiguration createConfiguration(DBSObject launchable) throws CoreException
    {
        ILaunchConfigurationWorkingCopy workingCopy = PostgreSqlDebugCore.createConfiguration(launchable);
        return workingCopy.doSave();
    }

}
