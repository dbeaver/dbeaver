package org.jkiss.dbeaver.tasks.nativetool.messages;

import org.eclipse.osgi.util.NLS;

public class NativeToolMessages extends NLS {

    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.tasks.nativetool.messages.NativeToolMessages";

    public static String native_tool_handler_log_task;
    public static String native_tool_handler_log_finished_task;

    static {
        NLS.initializeMessages(BUNDLE_NAME, NativeToolMessages.class);
    }

    private NativeToolMessages() {
    }
}
