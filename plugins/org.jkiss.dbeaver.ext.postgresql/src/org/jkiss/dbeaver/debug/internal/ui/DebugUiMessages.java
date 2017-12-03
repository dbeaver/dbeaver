package org.jkiss.dbeaver.debug.internal.ui;

import org.eclipse.osgi.util.NLS;

public class DebugUiMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.debug.internal.ui.DebugUiMessages"; //$NON-NLS-1$
    public static String DatabaseTab_database_group_text;
    public static String DatabaseTab_database_label_text;
    public static String DatabaseTab_datasource_group_text;
    public static String DatabaseTab_datasource_label_text;
    public static String DatabaseTab_name;
    public static String DebugContributionFactory_text;
    public static String LaunchShortcut_e_launch;
    public static String LaunchShortcut_select_cobfiguration_title;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, DebugUiMessages.class);
    }

    private DebugUiMessages()
    {
    }
}
