package org.jkiss.dbeaver.runtime.internal.ide.core;

import org.eclipse.osgi.util.NLS;

public class IdeCoreMessages extends NLS {

	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.runtime.internal.ide.core.ide_core_messages"; //$NON-NLS-1$

	public static String CreateLinkedFileRunnable_e_unable_to_link;
	public static String CreateLinkedFolderRunnable_e_unable_to_link;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, IdeCoreMessages.class);
	}

	private IdeCoreMessages()
	{
	}
}
