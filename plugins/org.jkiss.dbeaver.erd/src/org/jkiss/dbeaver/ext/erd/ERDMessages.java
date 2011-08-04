/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd;

import org.eclipse.osgi.util.NLS;

public class ERDMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.erd.DBeaverResources"; //$NON-NLS-1$

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ERDMessages.class);
	}

    public static String diagram_create_wizard;
    public static String create_new_diagram;
    public static String manage_diagram_content;
    public static String settings;
    public static String name;
    public static String initial_content_optional;
    public static String diagrams;
    public static String diagram_folder;
    public static String diagram;

    public static String column_;
    public static String entity_diagram_;
    public static String note;

	private ERDMessages() {
	}
}
