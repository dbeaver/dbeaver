/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Liu, Yuanyuan (liuyuanyuan@highgo.com)
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
package org.jkiss.dbeaver.ui.editors.acl.internal;

import org.eclipse.osgi.util.NLS;

public class ACLMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.acl.internal.ACLMessages"; //$NON-NLS-1$
		
	/* Permissions */
	public static String edit_command_grant_privilege_action_grant_privilege;
	public static String edit_command_grant_privilege_action_revoke_privilege;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ACLMessages.class);
	}

	private ACLMessages() {
	}
	
}
