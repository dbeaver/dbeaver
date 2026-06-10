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
package org.jkiss.dbeaver.ui.config.easy.nls;

import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIObservables;

@SuppressWarnings("CheckStyle")
public final class EasyConfigMessages {
    // @formatter:off
    public static final UIObservable<String> welcome_title = UIObservables.computed(() -> Raw.easy_config_welcome_title, String.class);
    public static final UIObservable<String> welcome_description = UIObservables.computed(() -> Raw.easy_config_welcome_description, String.class);
    public static final UIObservable<String> welcome_body_text = UIObservables.computed(() -> Raw.easy_config_welcome_body_text, String.class);

    public static final UIObservable<String> appearance_title = UIObservables.computed(() -> Raw.easy_config_appearance_title, String.class);
    public static final UIObservable<String> appearance_description = UIObservables.computed(() -> Raw.easy_config_appearance_description, String.class);

    public static final UIObservable<String> security_title = UIObservables.computed(() -> Raw.easy_config_security_title, String.class);
    public static final UIObservable<String> security_description = UIObservables.computed(() -> Raw.easy_config_security_description, String.class);

    public static final UIObservable<String> features_title = UIObservables.computed(() -> Raw.easy_config_features_title, String.class);
    public static final UIObservable<String> features_description = UIObservables.computed(() -> Raw.easy_config_features_description, String.class);

    public static final UIObservable<String> data_collection_title = UIObservables.computed(() -> Raw.easy_config_data_collection_title, String.class);
    public static final UIObservable<String> data_collection_description = UIObservables.computed(() -> Raw.easy_config_data_collection_description, String.class);
    public static final UIObservable<String> data_collection_agreement_text = UIObservables.computed(() -> Raw.easy_config_data_collection_agreement_text, String.class);
    public static final UIObservable<String> data_collection_send_usage_statistics = UIObservables.computed(() -> Raw.easy_config_data_collection_send_usage_statistics, String.class);

    public static final UIObservable<String> final_steps_title = UIObservables.computed(() -> Raw.easy_config_final_steps_title, String.class);
    public static final UIObservable<String> final_steps_description = UIObservables.computed(() -> Raw.easy_config_final_steps_description, String.class);
    // @formatter:on

    private EasyConfigMessages() {
    }

    public static void reload() {
        Raw.reload();
    }

    static class Raw extends ReloadableNLS {
        public static String easy_config_welcome_title;
        public static String easy_config_welcome_description;
        public static String easy_config_welcome_body_text;

        public static String easy_config_appearance_title;
        public static String easy_config_appearance_description;

        public static String easy_config_security_title;
        public static String easy_config_security_description;

        public static String easy_config_features_title;
        public static String easy_config_features_description;

        public static String easy_config_data_collection_title;
        public static String easy_config_data_collection_description;
        public static String easy_config_data_collection_agreement_text;
        public static String easy_config_data_collection_send_usage_statistics;

        public static String easy_config_final_steps_title;
        public static String easy_config_final_steps_description;

        static {
            ReloadableNLS.initializeMessages(EasyConfigMessages.class.getName(), Raw.class);
        }

        private Raw() {
        }

        static void reload() {
            ReloadableNLS.reloadMessages(EasyConfigMessages.class.getName(), Raw.class);
        }
    }
}
