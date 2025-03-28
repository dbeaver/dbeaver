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
package org.jkiss.dbeaver.ext.vertica.internal;

import org.jkiss.dbeaver.utils.NLS;

public class VerticaMessages extends NLS {

    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.vertica.internal.VerticaMessages"; //$NON-NLS-1$

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, VerticaMessages.class);
    }

    private VerticaMessages() {
        // prevents construction
    }

    public static String vertica_password_will_expire_warn_name;
    public static String vertica_password_will_expire_warn_description;
    public static String data_source_prompt_to_change_pass_title;
    public static String data_source_prompt_to_change_pass_message;

}
