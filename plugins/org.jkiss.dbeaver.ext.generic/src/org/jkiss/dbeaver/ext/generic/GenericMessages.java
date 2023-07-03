package org.jkiss.dbeaver.ext.generic;

import org.eclipse.osgi.util.NLS;

public class GenericMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.generic.GenericMessages";
	
	public static String generic_object_container_none;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, GenericMessages.class);
	}

	private GenericMessages() {
	}

}
