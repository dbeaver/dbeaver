package org.jkiss.dbeaver.debug.internal.core;

import org.eclipse.osgi.util.NLS;

public class DebugCoreMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.debug.internal.core.DebugCoreMessages"; //$NON-NLS-1$
    public static String DatabaseDebugController_debug_context_purpose;
    public static String DatabaseDebugController_debug_session_name;
    public static String DatabaseDebugController_e_connecting_datasource;
    public static String DatabaseDebugController_e_opening_debug_context;
    public static String DebugCore_e_read_attribute_generic;
    public static String DebugCore_e_read_attribute_null;
    public static String DebugCore_e_unable_to_retrieve_modes;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, DebugCoreMessages.class);
    }

    private DebugCoreMessages()
    {
    }
}
