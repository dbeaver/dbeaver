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
package org.jkiss.dbeaver.ui.statistics.easyConfig;

import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.util.UIReloadableNLS;

public final class EasyConfigMessages extends UIReloadableNLS {
    public static UIObservable<String> data_collection_title;
    public static UIObservable<String> data_collection_description;
    public static UIObservable<String> data_collection_agreement_text;
    public static UIObservable<String> data_collection_send_usage_statistics;

    static {
        UIReloadableNLS.initializeMessages(EasyConfigMessages.class.getName(), EasyConfigMessages.class);
    }

    private EasyConfigMessages() {
    }
}
