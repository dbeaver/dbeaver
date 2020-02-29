/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.net.ssh;

import org.eclipse.osgi.util.NLS;

public class SSHUIMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.net.ssh.SSHUIMessages"; //$NON-NLS-1$

	public static String model_ssh_configurator_group_settings;
	public static String model_ssh_configurator_group_advanced;
	public static String model_ssh_configurator_checkbox_save_pass;
	public static String model_ssh_configurator_combo_auth_method;
	public static String model_ssh_configurator_combo_password;
	public static String model_ssh_configurator_combo_pub_key;
	public static String model_ssh_configurator_dialog_choose_private_key;
	public static String model_ssh_configurator_label_host_ip;
	public static String model_ssh_configurator_label_password;
	public static String model_ssh_configurator_label_passphrase;
	public static String model_ssh_configurator_label_port;
	public static String model_ssh_configurator_label_private_key;
	public static String model_ssh_configurator_label_user_name;
	public static String model_ssh_configurator_label_implementation;
	public static String model_ssh_configurator_label_local_host;
	public static String model_ssh_configurator_label_local_host_description;
	public static String model_ssh_configurator_label_local_port;
	public static String model_ssh_configurator_label_local_port_description;
	public static String model_ssh_configurator_label_remote_host;
	public static String model_ssh_configurator_label_remote_host_description;
	public static String model_ssh_configurator_label_remote_port;
	public static String model_ssh_configurator_label_remote_port_description;
    public static String model_ssh_configurator_label_keep_alive;
	public static String model_ssh_configurator_label_tunnel_timeout;
	public static String model_ssh_configurator_button_test_tunnel;
	public static String model_ssh_configurator_combo_agent;


	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, SSHUIMessages.class);
	}

	private SSHUIMessages() {
	}
}
