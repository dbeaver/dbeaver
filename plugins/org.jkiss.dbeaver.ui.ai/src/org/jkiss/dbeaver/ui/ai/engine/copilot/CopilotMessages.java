/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.engine.copilot;

import org.eclipse.osgi.util.NLS;

public class CopilotMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.ai.engine.copilot.CopilotMessages"; //$NON-NLS-1$

    public static String copilot_preference_page_token_info;
    public static String copilot_access_token;
    public static String copilot_access_token_authorize;
    public static String oauth_auth_title;
    public static String oauth_auth_success_message;
    public static String oauth_auth_error_message;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, CopilotMessages.class);
    }

    private CopilotMessages() {
    }

}
