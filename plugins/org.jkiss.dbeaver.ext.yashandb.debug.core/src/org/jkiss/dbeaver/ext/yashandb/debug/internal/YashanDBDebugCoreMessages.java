
package org.jkiss.dbeaver.ext.yashandb.debug.internal;

import org.eclipse.osgi.util.NLS;

//TODO: 待完善
public class YashanDBDebugCoreMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.yashandb.debug.internal.YashanDBDebugCoreMessages"; //$NON-NLS-1$
    public static String YashanDBDebugController_connection_application_name;
    public static String YashanDBDebugController_e_failed_session_close;
    public static String YashanDBDebugController_e_failed_session_open;

    public static String YashanDBSqlDebugCore_e_procedure_required;
    public static String YashanDBSqlDebugCore_launch_configuration_name;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, YashanDBDebugCoreMessages.class);
    }

    private YashanDBDebugCoreMessages() {
    }
}
