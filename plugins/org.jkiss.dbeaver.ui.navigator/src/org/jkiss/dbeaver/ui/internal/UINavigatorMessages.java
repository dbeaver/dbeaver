/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.internal;

import org.eclipse.osgi.util.NLS;

public class UINavigatorMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.internal.UINavigatorMessages"; //$NON-NLS-1$

    //object properties editor
    public static String obj_editor_properties_control_action_filter_setting;
    public static String obj_editor_properties_control_action_configure_columns;
    public static String obj_editor_properties_control_action_configure_columns_description;
    //object properties editor

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UINavigatorMessages.class);
    }

    private UINavigatorMessages() {
    }
}
