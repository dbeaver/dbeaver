/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.internal;

import org.eclipse.osgi.util.NLS;

public final class AltibaseMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.altibase.internal.AltibaseMessages"; //$NON-NLS-1$

    public static String sql_warning_title;
    public static String password_expire_warning;

    static {
        NLS.initializeMessages(BUNDLE_NAME, AltibaseMessages.class);
    }

    private AltibaseMessages() {
    }
}
