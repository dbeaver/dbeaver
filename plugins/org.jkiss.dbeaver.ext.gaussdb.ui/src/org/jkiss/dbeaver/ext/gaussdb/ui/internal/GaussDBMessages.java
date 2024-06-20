package org.jkiss.dbeaver.ext.gaussdb.ui.internal;

import org.eclipse.osgi.util.NLS;

public class GaussDBMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.gaussdb.ui.internal.GaussDBResources";
    public static String        dialog_struct_create_procedure_combo_type;
    public static String        dialog_struct_create_procedure_label_name;
    public static String        dialog_struct_create_procedure_title;
    public static String        dialog_struct_create_function_title;
    public static String        dialog_struct_create_procedure_container;

    public static String        tree_procedures_node_name;
    public static String        tree_functions_node_name;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, GaussDBMessages.class);
    }

    private GaussDBMessages() {
    }
}