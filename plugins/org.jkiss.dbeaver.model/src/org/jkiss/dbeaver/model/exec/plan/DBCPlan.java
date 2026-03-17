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

package org.jkiss.dbeaver.model.exec.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

import java.util.List;
import java.util.Map;

/**
 * Execution plan
 */
public interface DBCPlan {

    /**
     * Keep original plan nodes structure (produced by server).
     * Restricts any client-side plan structure modifications.
     */
    String OPTION_KEEP_ORIGINAL = "keepOriginal";

    @NotNull
    String getQueryString();

    @NotNull
    String getPlanQueryString() throws DBException;

    @NotNull
    DBCPlanSourceFormat getPlanSourceDataFormat();

    @Nullable
    Object getPlanSourceData();

    @Nullable
    Object getPlanFeature(@NotNull String feature);

    @NotNull
    List<? extends DBCPlanNode> getPlanNodes(@NotNull Map<String, Object> options);

}
