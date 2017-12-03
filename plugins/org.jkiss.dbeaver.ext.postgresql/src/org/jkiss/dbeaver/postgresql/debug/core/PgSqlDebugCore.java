package org.jkiss.dbeaver.postgresql.debug.core;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.ext.postgresql.PostgreActivator;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class PgSqlDebugCore {
    
    public static final String MODEL_IDENTIFIER = "org.jkiss.dbeaver.postgresql.debug.core"; //$NON-NLS-1$

    public static final String CONFIGURATION_TYPE = MODEL_IDENTIFIER + '.' + "pgSQL";//$NON-NLS-1$

    public static final String ATTR_OID = MODEL_IDENTIFIER + '.' + "ATTR_OID"; //$NON-NLS-1$

    public static final String ATTR_OID_DEFAULT = ""; //$NON-NLS-1$

    public static final String BUNDLE_SYMBOLIC_NAME = PostgreActivator.PLUGIN_ID;
    
    public static CoreException abort(String message) {
        return abort(message, null);
    }

    public static CoreException abort(String message, Throwable th) {
        return new CoreException(newErrorStatus(message, th));
    }

    public static Status newErrorStatus(String message) {
        return newErrorStatus(message, null);
    }

    public static Status newErrorStatus(String message, Throwable th) {
        return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message, th) ;
    }

    public static ILaunchConfigurationWorkingCopy createConfiguration(DBSObject launchable)
            throws CoreException {
        boolean isInstance = launchable instanceof PostgreProcedure;
        if (!isInstance) {
            throw PgSqlDebugCore.abort("PostgreSQL procedure is required to create PL/pgSQL launch configuration");
        }
        PostgreProcedure procedure = (PostgreProcedure) launchable;
        PostgreDataSource dataSource = procedure.getDataSource();
        DBPDataSourceContainer dataSourceContainer = dataSource.getContainer();
        PostgreDatabase database = procedure.getDatabase();
        PostgreSchema schema = procedure.getContainer();
        
        String databaseName = database.getName();
        Object[] bindings = new Object[] {dataSourceContainer.getName(), databaseName,
                procedure.getName(), schema.getName()};
        String name = NLS.bind("{0}({1}) - {2}({3})", bindings);
        //Let's use metadata area for storage
        IContainer container = null;
        ILaunchConfigurationWorkingCopy workingCopy = createConfiguration(container, name);
        workingCopy.setAttribute(DebugCore.ATTR_DATASOURCE, dataSourceContainer.getId());
        workingCopy.setAttribute(DebugCore.ATTR_DATABASE, databaseName);
        workingCopy.setAttribute(ATTR_OID, String.valueOf(procedure.getObjectId()));
        return workingCopy;
    }

    public static ILaunchConfigurationWorkingCopy createConfiguration(IContainer container, String name)
            throws CoreException {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager.getLaunchConfigurationType(CONFIGURATION_TYPE);
        ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(container, name);
        return workingCopy;
    }
}
