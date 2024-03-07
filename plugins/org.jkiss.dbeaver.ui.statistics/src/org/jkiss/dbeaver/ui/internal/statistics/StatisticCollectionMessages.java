/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.internal.statistics;

import org.eclipse.osgi.util.NLS;

public class StatisticCollectionMessages extends NLS {

    public static String statistic_collection_dialog_title;
    public static String statistic_collection_pref_link;
    public static String statistic_collection_dont_share_lbl;
    public static String statistic_collection_confirm_lbl;
    public static String statistic_collection_pref_group_label;
    public static String statistic_collection_pref_send_btn_label;
    public static String statistic_collection_pref_content_main_msg;
    public static String statistic_collection_pref_content_documentation_link;
    public static String statistic_collection_pref_content_opensource_link;
    public static String statistic_collection_pref_content_datashare_msg;

    static {
        NLS.initializeMessages(StatisticCollectionMessages.class.getName(), StatisticCollectionMessages.class);
    }

    private StatisticCollectionMessages() {
    }
}
