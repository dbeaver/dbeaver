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
package org.jkiss.dbeaver.model.data.order;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.data.messages.DataMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

public enum OrderingPolicy {
    DEFAULT(DataMessages.database_resultsets_label_order_policy_default),
    PRIMARY_KEY_ASC(DataMessages.database_resultsets_label_order_policy_primary_key_asc),
    PRIMARY_KEY_DESC(DataMessages.database_resultsets_label_order_policy_primary_key_desc);

    private final String text;

    OrderingPolicy(String text) {
        this.text = text;
    }

    @NotNull
    public static OrderingPolicy get(@NotNull DBPPreferenceStore store) {
        String value = store.getString(ModelPreferences.RESULT_SET_ORDERING_POLICY);
        return CommonUtils.valueOf(OrderingPolicy.class, value, DEFAULT);
    }

    @NotNull
    public String getText() {
        return text;
    }
}
