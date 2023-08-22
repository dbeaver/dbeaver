package org.jkiss.dbeaver.ext.yashandb.internal;

import org.eclipse.osgi.util.NLS;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.yashandb.internal.YashanDBMessages";

    static {
        // initialize resource bundle.
        NLS.initializeMessages(BUNDLE_NAME, YashanDBMessages.class);
    }

    private YashanDBMessages() {
    }
    
    public static String edit_yashandb_dependencies_dependency_name;
    public static String edit_yashandb_dependencies_dependency_description;
    public static String edit_yashandb_dependencies_dependent_name;
    public static String edit_yashandb_dependencies_dependent_description;
}
