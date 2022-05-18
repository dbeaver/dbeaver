/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.controls.StatusLineContributionItemEx;

import java.util.HashMap;
import java.util.Map;

public class StatusLineContributionItemsRegistry {
    private static final Map<String, StatusLineContributionItemEx> items = new HashMap<>();

    @NotNull
    public static StatusLineContributionItemEx getInstanceOfItem(String id) {
        StatusLineContributionItemEx item = items.get(id);
        if (item == null) {
            StatusLineContributionItemEx statusLineContributionItemEx = new StatusLineContributionItemEx(id);
            items.put(id, statusLineContributionItemEx);
            return statusLineContributionItemEx;
        } else {
            return item;
        }
    }
}
