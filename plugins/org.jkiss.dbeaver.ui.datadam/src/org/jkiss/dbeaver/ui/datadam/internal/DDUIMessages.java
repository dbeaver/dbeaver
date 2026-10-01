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
package org.jkiss.dbeaver.ui.datadam.internal;

import org.eclipse.osgi.util.NLS;

public class DDUIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.datadam.internal.DDUIMessages"; //$NON-NLS-1$

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, DDUIMessages.class);
    }

    public static String datadam_configurator_label_api_key;
    public static String datadam_configurator_label_endpoint;
    public static String datadam_configurator_label_temperature;
    public static String datadam_configurator_log_query_label;
    public static String datadam_configurator_log_query_tip;

    private DDUIMessages() {
    }
}
