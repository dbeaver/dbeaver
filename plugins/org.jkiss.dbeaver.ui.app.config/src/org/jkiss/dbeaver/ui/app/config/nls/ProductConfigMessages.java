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
package org.jkiss.dbeaver.ui.app.config.nls;

import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.util.UIReloadableNLS;

public final class ProductConfigMessages extends UIReloadableNLS {
    public static UIObservable<String> welcome_title;
    public static UIObservable<String> welcome_description;
    public static UIObservable<String> welcome_body_text;

    public static UIObservable<String> appearance_title;
    public static UIObservable<String> appearance_description;
    public static UIObservable<String> appearance_theme_header;
    public static UIObservable<String> appearance_theme_hint;

    public static UIObservable<String> features_title;
    public static UIObservable<String> features_description;
    public static UIObservable<String> features_list_header;

    public static UIObservable<String> final_steps_title;
    public static UIObservable<String> final_steps_description;
    public static UIObservable<String> final_steps_header;

    public static UIObservable<String> wizard_buttons_back;
    public static UIObservable<String> wizard_buttons_next;
    public static UIObservable<String> wizard_buttons_finish;

    static {
        UIReloadableNLS.initializeMessages(ProductConfigMessages.class.getName(), ProductConfigMessages.class);
    }

    private ProductConfigMessages() {
    }
}
