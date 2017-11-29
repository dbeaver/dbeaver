/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.runtime.internal.ide.core;

import org.eclipse.osgi.util.NLS;

public class IdeCoreMessages extends NLS {

	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.runtime.internal.ide.core.ide_core_messages"; //$NON-NLS-1$

	public static String CreateLinkedFileRunnable_e_cancelled_link;

    public static String CreateLinkedFileRunnable_e_unable_to_link;
    
	public static String CreateLinkedFolderRunnable_e_cancelled_link;

    public static String CreateLinkedFolderRunnable_e_unable_to_link;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, IdeCoreMessages.class);
	}

	private IdeCoreMessages()
	{
	}
}
