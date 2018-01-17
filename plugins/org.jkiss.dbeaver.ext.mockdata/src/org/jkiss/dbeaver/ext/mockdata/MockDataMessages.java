/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mockdata;

import org.eclipse.osgi.util.NLS;

public class MockDataMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.mockdata.MockDataResources"; //$NON-NLS-1$

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, MockDataMessages.class);
    }

    public MockDataMessages() {
    }

    public static String tools_mockdata_generate_data_task;

    public static String tools_mockdata_wizard_title;
    public static String tools_mockdata_wizard_page_name;
    public static String tools_mockdata_wizard_message_process_completed;

    public static String tools_mockdata_wizard_page_settings_group_settings;
    public static String tools_mockdata_wizard_page_settings_page_name;
    public static String tools_mockdata_wizard_page_settings_page_description;
    public static String tools_mockdata_wizard_page_settings_checkbox_remove_old_data;

    public static String tools_mockdata_confirm_delete_old_data_message;

}
