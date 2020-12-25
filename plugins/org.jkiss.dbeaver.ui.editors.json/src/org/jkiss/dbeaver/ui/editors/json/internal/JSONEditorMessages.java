package org.jkiss.dbeaver.ui.editors.json.internal;

import org.eclipse.osgi.util.NLS;

public class JSONEditorMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.json.internal.JSONEditorMessages"; //$NON-NLS-1$

    public static String JSONEditorPart_title;
    public static String JSONPanelEditor_e_load_json;
    public static String JSONPanelEditor_e_save_json;
    public static String JSONPanelEditor_subtask_prime_task;
    public static String JSONPanelEditor_task_prime;
    public static String JSONPanelEditor_task_read_json;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, JSONEditorMessages.class);
    }

    private JSONEditorMessages()
    {
    }
}
