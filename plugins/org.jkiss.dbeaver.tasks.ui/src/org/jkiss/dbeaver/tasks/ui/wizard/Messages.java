package org.jkiss.dbeaver.tasks.ui.wizard;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.tasks.ui.wizard.messages"; //$NON-NLS-1$
	public static String TaskConfigurationWizard___cant_find_task_type;
	public static String TaskConfigurationWizard___error_opening_DB_tasks_view;
	public static String TaskConfigurationWizard___error_saving_task_config;
	public static String TaskConfigurationWizard___no_task_type;
	public static String TaskConfigurationWizard___open_tasks_view;
	public static String TaskConfigurationWizard___save_task;
	public static String TaskConfigurationWizard___show_view;
	public static String TaskConfigurationWizard___Task_run_error;
	public static String TaskConfigurationWizard___task_save_error;
	public static String TaskConfigurationWizard___variables;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
