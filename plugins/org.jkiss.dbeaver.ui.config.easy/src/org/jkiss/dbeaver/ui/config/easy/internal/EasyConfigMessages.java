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
package org.jkiss.dbeaver.ui.config.easy.internal;

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.forms.UIObservable;

import java.lang.reflect.Field;

public final class EasyConfigMessages {
    public static final UIObservable<String> welcome_title = UIObservable.of("");
    public static final UIObservable<String> welcome_description = UIObservable.of("");
    public static final UIObservable<String> welcome_body_title = UIObservable.of("");
    public static final UIObservable<String> welcome_body_text = UIObservable.of("");
    public static final UIObservable<String> welcome_language = UIObservable.of("");

    public static final UIObservable<String> appearance_title = UIObservable.of("");
    public static final UIObservable<String> appearance_description = UIObservable.of("");

    public static final UIObservable<String> security_title = UIObservable.of("");
    public static final UIObservable<String> security_description = UIObservable.of("");

    public static final UIObservable<String> features_title = UIObservable.of("");
    public static final UIObservable<String> features_description = UIObservable.of("");

    public static final UIObservable<String> data_collection_title = UIObservable.of("");
    public static final UIObservable<String> data_collection_description = UIObservable.of("");

    public static final UIObservable<String> sample_database_title = UIObservable.of("");
    public static final UIObservable<String> sample_database_description = UIObservable.of("");

    static {
        sync();
    }

    private EasyConfigMessages() {
    }

    public static void reload() {
        Holder.initialize();
        sync();
    }

    private static void sync() {
        welcome_title.set(Holder.easy_config_welcome_title);
        welcome_description.set(Holder.easy_config_welcome_description);
        welcome_body_title.set(Holder.easy_config_welcome_body_title);
        welcome_body_text.set(Holder.easy_config_welcome_body_text);
        welcome_language.set(Holder.easy_config_welcome_language);
        appearance_title.set(Holder.easy_config_appearance_title);
        appearance_description.set(Holder.easy_config_appearance_description);
        security_title.set(Holder.easy_config_security_title);
        security_description.set(Holder.easy_config_security_description);
        features_title.set(Holder.easy_config_features_title);
        features_description.set(Holder.easy_config_features_description);
        data_collection_title.set(Holder.easy_config_data_collection_title);
        data_collection_description.set(Holder.easy_config_data_collection_description);
        sample_database_title.set(Holder.easy_config_sample_database_title);
        sample_database_description.set(Holder.easy_config_sample_database_description);
    }

    static class Holder extends NLS {
        private static final Log log = Log.getLog(Holder.class);

        public static String easy_config_welcome_title;
        public static String easy_config_welcome_description;
        public static String easy_config_welcome_body_title;
        public static String easy_config_welcome_body_text;
        public static String easy_config_welcome_language;

        public static String easy_config_appearance_title;
        public static String easy_config_appearance_description;

        public static String easy_config_security_title;
        public static String easy_config_security_description;

        public static String easy_config_features_title;
        public static String easy_config_features_description;

        public static String easy_config_data_collection_title;
        public static String easy_config_data_collection_description;

        public static String easy_config_sample_database_title;
        public static String easy_config_sample_database_description;

        static {
            initialize();
        }

        private Holder() {
        }

        static void initialize() {
            try {
                // OSGI caches the suffixes, so we need to reset it to reload messages
                Field field = NLS.class.getDeclaredField("nlSuffixes");
                field.setAccessible(true);
                field.set(null, null);
            } catch (Exception e) {
                log.error("Failed to reset NLS cache", e);
            }

            NLS.initializeMessages(EasyConfigMessages.class.getName(), Holder.class);
        }
    }
}
