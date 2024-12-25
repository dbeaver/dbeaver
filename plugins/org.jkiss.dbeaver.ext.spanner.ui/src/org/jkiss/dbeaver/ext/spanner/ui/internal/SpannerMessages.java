/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.spanner.ui.internal;

import org.jkiss.dbeaver.utils.NLS;

public class SpannerMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.spanner.ui.internal.messages"; //$NON-NLS-1$

	public static String label_connection;
	public static String label_private_key_path;
	public static String label_project;
	public static String label_instance;
	public static String label_instance_tip;
	public static String label_database;
	public static String label_database_tip;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, SpannerMessages.class);
	}

	private SpannerMessages() {
	}
}
