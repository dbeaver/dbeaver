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
package org.jkiss.dbeaver.model.sql.semantics.completion;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionActivityTracker;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.completion.usage.SQLCompletionUsageManager;
import org.jkiss.dbeaver.model.sql.completion.usage.SQLUsageContext;
import org.jkiss.dbeaver.model.sql.completion.usage.SQLUsageExtractor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

public class SQLQueryCompletionProposalContext {

    private final SQLCompletionRequest completionRequest;
    private final int requestOffset;

    private DBPDataSourceContainer usageContainer;
    private SQLUsageContext usageContext;
    private List<String> usageShown;

    public SQLQueryCompletionProposalContext(@NotNull SQLCompletionRequest completionRequest, int requestOffset) {
        this.completionRequest = completionRequest;
        this.requestOffset = requestOffset;
    }

    @NotNull
    public SQLCompletionContext getCompletionContext() {
        return this.completionRequest.getContext();
    }

    public int getRequestOffset() {
        return this.requestOffset;
    }

    public boolean isInsertSpaceAfterProposal() {
        return false;
    }

    @NotNull
    public SQLCompletionActivityTracker getActivityTracker() {
        return this.completionRequest.getActivityTracker();
    }

    /**
     * Remembers the proposals ranked by usage, so the ranking can learn from the chosen one.
     *
     * @param shown keys of the ranked proposals, in the order shown
     */
    void setUsage(@NotNull DBPDataSourceContainer container, @NotNull SQLUsageContext context, @NotNull List<String> shown) {
        this.usageContainer = container;
        this.usageContext = context;
        this.usageShown = shown;
    }

    /**
     * Learns that the user chose the proposal of the object.
     */
    public void learnChoice(@Nullable DBSObject object) {
        String key = SQLUsageExtractor.objectKey(object);
        if (usageContainer != null && key != null && usageShown.contains(key)) {
            SQLCompletionUsageManager.getInstance().learnChoice(usageContainer, usageContext, usageShown, key);
        }
    }
}
