package org.jkiss.dbeaver.postgresql.internal.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.launch.ui.LaunchShortcut;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.postgresql.debug.core.PgSqlDebugCore;

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
        return PgSqlDebugCore.CONFIGURATION_TYPE;
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
        try {
            String datasource = config.getAttribute(PgSqlDebugCore.ATTR_DATASOURCE, ""); //$NON-NLS-1$
            String id = launchable.getDataSource().getContainer().getId();
            if (!datasource.equals(id)) {
                return false;
            }
            String database = config.getAttribute(PgSqlDebugCore.ATTR_DATABASE, ""); //$NON-NLS-1$
            String databaseName = launchable.getDataSource().getName();
            if (!database.equals(databaseName)) {
                return false;
            }
            String oid = config.getAttribute(PgSqlDebugCore.ATTR_OID, String.valueOf(0));
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
        ILaunchConfigurationWorkingCopy workingCopy = PgSqlDebugCore.createConfiguration(launchable);
        return workingCopy.doSave();
    }

}
