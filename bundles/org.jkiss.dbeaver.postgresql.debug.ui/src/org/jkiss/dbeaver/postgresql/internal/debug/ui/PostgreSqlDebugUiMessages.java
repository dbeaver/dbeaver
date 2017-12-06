package org.jkiss.dbeaver.postgresql.internal.debug.ui;

import org.eclipse.osgi.util.NLS;

public class PostgreSqlDebugUiMessages extends NLS {

    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.postgresql.internal.debug.ui.PostgreSqlDebugUiMessages"; //$NON-NLS-1$
    
    public static String PgSqlLaunchShortcut_e_editor_empty;
    public static String PgSqlLaunchShortcut_e_selection_empty;
    public static String PgSqlLaunchShortcut_select_procedure_message;
    public static String PgSqlLaunchShortcut_select_procedure_title;
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, PostgreSqlDebugUiMessages.class);
    }

    private PostgreSqlDebugUiMessages()
    {
    }
}
