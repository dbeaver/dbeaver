package org.jkiss.dbeaver.ext.sample.database;

import org.eclipse.osgi.util.NLS;

public class SampleDatabaseMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.sample.database.SampleDatabaseResources"; //$NON-NLS-1$

    public static String dialog_create_title;
    public static String dialog_create_description;
    public static String dialog_already_created_title;
    public static String dialog_already_created_description;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, SampleDatabaseMessages.class);
    }

    private SampleDatabaseMessages() {
    }
}
