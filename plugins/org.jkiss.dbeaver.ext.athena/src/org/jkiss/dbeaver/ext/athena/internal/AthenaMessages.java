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
package org.jkiss.dbeaver.ext.athena.internal;

import org.eclipse.osgi.util.NLS;

public class AthenaMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.athena.internal.AthenaMessages"; //$NON-NLS-1$

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, AthenaMessages.class);
	}

	public static String label_region;
	public static String label_connection;
	public static String label_aws_access_key;

	private AthenaMessages() {
	}

	public static String label_access_key;
	public static String label_access_key_id;
	public static String label_s3_location;
	public static String label_s3_output_location;
	public static String label_secret_key;
	public static String label_security;
}
