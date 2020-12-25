package org.jkiss.dbeaver.ext.import_config;

import org.eclipse.osgi.util.NLS;

public class ImportConfigMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.import_config.ImportConfigMessages"; //$NON-NLS-1$
	public static String config_import_wizard_header_import_configuration;
	public static String config_import_wizard_page_caption_connections;
	public static String config_import_wizard_page_dbvis_label_installation_not_found;
	public static String config_import_wizard_page_label_connection_list;
	public static String config_import_wizard_page_squirrel_label_installation_not_found;
	public static String config_import_wizard_page_th_driver;
	public static String config_import_wizard_page_th_name;
	public static String config_import_wizard_page_th_url;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ImportConfigMessages.class);
	}

	private ImportConfigMessages() {
	}
}
