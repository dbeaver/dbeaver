package org.jkiss.dbeaver.runtime.ide.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class IdeCore {
	
	public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.runtime.ide.core"; //$NON-NLS-1$
	
	public static IStatus createError(String message) {
		return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message);
	}

	public static IStatus createError(String message, Throwable t) {
		return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message, t);
	}

}
