package org.jkiss.dbeaver.ext.mysql;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;

public interface SecondaryStepAuthProvider {
    
    static final Log log = Log.getLog(SecondaryStepAuthProvider.class);
    
    String obtainSecondaryPassword(String reason);
    
    public static SecondaryStepAuthProvider getSecondaryAuthProvider() {
        IExtensionPoint ep = Platform.getExtensionRegistry().getExtensionPoint("org.jkiss.dbeaver.ext.mysql.auth.secondaryStepDialogProvider");
        if (ep != null) {
            IExtension[] exts =  ep.getExtensions();
            if (exts.length > 0) {
                IConfigurationElement[] elts = exts[0].getConfigurationElements();
                if (elts.length > 0) {
                    try {
                        return (SecondaryStepAuthProvider)elts[0].createExecutableExtension("class");
                    } catch (CoreException e) {
                        log.error("Failed to load SecondaryStepDialogProvider", e);
                    }
                }
            }
        }
        return null;
    }
}
