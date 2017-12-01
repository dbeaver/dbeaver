package org.jkiss.dbeaver.postgresql.internal.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.jkiss.dbeaver.launch.ui.LaunchShortcut;
import org.jkiss.dbeaver.model.struct.DBSObject;

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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean isCandidate(ILaunchConfiguration config, DBSObject launchable)
    {
        if (!config.exists()) {
            return false;
        }
        return false;
    }

    @Override
    protected ILaunchConfiguration createConfiguration(DBSObject launchable, String mode) throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
