package org.jkiss.dbeaver.tools.compare.simple.messages;

import org.eclipse.osgi.util.NLS;

public class CompareMessages extends NLS {

    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.tools.compare.simple.messages.CompareMessages";

    public static String open_in_browser;
    public static String save_to_file;
    public static String compare_object_handler_error_just_one_object_selected;

    static {
        NLS.initializeMessages(BUNDLE_NAME, CompareMessages.class);
    }

    private CompareMessages() {
    }

}
