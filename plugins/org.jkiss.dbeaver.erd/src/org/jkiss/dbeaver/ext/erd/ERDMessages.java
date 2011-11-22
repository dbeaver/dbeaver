/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd;

import org.eclipse.osgi.util.NLS;

public class ERDMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.erd.ERDResources"; //$NON-NLS-1$

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ERDMessages.class);
	}

    public static String action_diagram_layout_name;
	public static String column_;
    public static String entity_diagram_;
    public static String part_note_title;
	public static String pref_page_erd_checkbox_grid_enabled;
	public static String pref_page_erd_checkbox_snap_to_grid;
    public static String pref_page_erd_combo_page_mode;
	public static String pref_page_erd_group_grid;
	public static String pref_page_erd_group_print;
	public static String pref_page_erd_item_fit_height;
	public static String pref_page_erd_item_fit_page;
	public static String pref_page_erd_item_fit_width;
	public static String pref_page_erd_item_tile;
	public static String pref_page_erd_spinner_grid_height;
	public static String pref_page_erd_spinner_grid_width;
	public static String pref_page_erd_spinner_margin_bottom;
	public static String pref_page_erd_spinner_margin_left;
	public static String pref_page_erd_spinner_margin_right;
	public static String pref_page_erd_spinner_margin_top;

    public static String wizard_diagram_create_title;

	public static String wizard_page_diagram_create_description;
	public static String wizard_page_diagram_create_group_settings;
	public static String wizard_page_diagram_create_label_init_content;
	public static String wizard_page_diagram_create_name;
	public static String wizard_page_diagram_create_title;

	private ERDMessages() {
	}
}
