/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.net.ssh;

import org.eclipse.osgi.util.NLS;

public final class SSHJUIMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.model.net.ssh.SSHJUIMessages"; //$NON-NLS-1$

    public static String verify_connection_confirmation_message;
    public static String verify_connection_confirmation_title;
    public static String known_host_added_warning_message;
    public static String warning_title;
    public static String host_key_changed_warning_message;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, SSHJUIMessages.class);
    }

    private SSHJUIMessages() {
    }
}

