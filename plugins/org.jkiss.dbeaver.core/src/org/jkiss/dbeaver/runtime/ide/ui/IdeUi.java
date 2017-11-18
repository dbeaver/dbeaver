package org.jkiss.dbeaver.runtime.ide.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class IdeUi {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.runtime.ide.ui"; //$NON-NLS-1$
    
    public static IStatus createError(String message) {
        return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message);
    }

    public static IStatus createError(String message, Throwable t) {
        return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message, t);
    }
}
