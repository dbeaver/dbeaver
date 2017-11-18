package org.jkiss.dbeaver.runtime.internal.ide.ui;

import org.eclipse.osgi.util.NLS;

public class IdeMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.runtime.internal.ide.ui.IdeMessages"; //$NON-NLS-1$
    public static String CreateLinkHandler_e_create_link_message;
    public static String CreateLinkHandler_e_create_link_title;
    public static String CreateLinkHandler_e_create_link_validation;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, IdeMessages.class);
    }

    private IdeMessages()
    {
    }
}
