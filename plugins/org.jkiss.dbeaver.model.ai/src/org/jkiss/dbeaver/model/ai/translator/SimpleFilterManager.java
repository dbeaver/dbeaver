/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.translator;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.utils.CommonUtils;

import java.time.LocalDate;
import java.util.*;

public class SimpleFilterManager implements DAIHistoryManager {
    private static final Map<String, List<DAIHistoryItem>> queryHistory = new HashMap<>();

    @NotNull
    @Override
    public List<DAIHistoryItem> readLastNaturalTexts(@NotNull DBSLogicalDataSource dataSource, int maxCount) {
        List<DAIHistoryItem> queries = queryHistory.get(dataSource.getDataSourceContainer().getId());
        if (!CommonUtils.isEmpty(queries)) {
            return new ArrayList<>(queries);
        }
        return Collections.emptyList();
    }

    @Override
    public void saveTranslationHistory(@NotNull DBSLogicalDataSource dataSource, @NotNull String natualText, @NotNull String sqlText) {
        List<DAIHistoryItem> queries = queryHistory.computeIfAbsent(dataSource.getDataSourceContainer().getId(), k -> new ArrayList<>());
        DAIHistoryItem item = new DAIHistoryItem(natualText, sqlText);
        item.setTime(LocalDate.now());
        queries.add(item);
    }
}
