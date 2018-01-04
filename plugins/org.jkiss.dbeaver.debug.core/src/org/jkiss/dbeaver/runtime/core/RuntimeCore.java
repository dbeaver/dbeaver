package org.jkiss.dbeaver.runtime.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.runtime.DBRResult;

public class RuntimeCore {
    
    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.runtime.core"; //$NON-NLS-1$

    public static IStatus toStatus(DBRResult result)
    {
        if (result == null) {
            return Status.CANCEL_STATUS;
        }
        int severity = toStatusSeverity(result.getSeverity());
        String message = result.getMessage();
        Throwable exception = result.getException();
        String pluginId = toPluginId();
        return new Status(severity, pluginId, message , exception );
    }

    private static String toPluginId()
    {
        return BUNDLE_SYMBOLIC_NAME;
    }
    
    public static int toStatusSeverity(int resultSeverity) {
        return resultSeverity;
    }

}
