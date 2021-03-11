package org.jkiss.dbeaver.ext.postgresql.internal;

import org.eclipse.osgi.util.NLS;

public class PostgreMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.postgresql.internal.PostgreMessages";

    static {
        NLS.initializeMessages(BUNDLE_NAME, PostgreMessages.class);
    }

    private PostgreMessages() {
    }

    public static String postgre_referential_integrity_disable_warning;
}
