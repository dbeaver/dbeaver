package org.jkiss.dbeaver.launch.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

public class LaunchCore {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.launch.core"; //$NON-NLS-1$

    public static boolean canLaunch(ILaunchConfiguration configuration, String mode) {
        if (configuration == null || !configuration.exists()) {
            return false;
        }
        try {
            return configuration.supportsMode(mode);
        } catch (CoreException e) {
            // ignore, we can not launch anyway
            return false;
        }       
    }
}
