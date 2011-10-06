/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle;

import org.eclipse.osgi.util.NLS;

public class OracleMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.oracle.OracleResources"; //$NON-NLS-1$

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, OracleMessages.class);
	}

	private OracleMessages() {
	}
}
