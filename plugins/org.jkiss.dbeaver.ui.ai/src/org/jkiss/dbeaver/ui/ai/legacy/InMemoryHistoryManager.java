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
package org.jkiss.dbeaver.ui.ai.legacy;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.qm.QMTranslationHistoryItem;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class InMemoryHistoryManager {
    private static final Map<String, List<QMTranslationHistoryItem>> queryHistory = new HashMap<>();

    @NotNull
    public static List<QMTranslationHistoryItem> readTranslationHistory(@NotNull DBSLogicalDataSource dataSource) {
        List<QMTranslationHistoryItem> queries = queryHistory.get(dataSource.getDataSourceContainer().getId());
        if (!CommonUtils.isEmpty(queries)) {
            return new ArrayList<>(queries);
        }
        return Collections.emptyList();
    }

    public static void saveTranslationHistory(
        @NotNull DBSLogicalDataSource dataSource,
        @NotNull QMTranslationHistoryItem item
    ) {
        queryHistory.computeIfAbsent(
            dataSource.getDataSourceContainer().getId(), s -> new ArrayList<>())
            .add(item);
    }

}
