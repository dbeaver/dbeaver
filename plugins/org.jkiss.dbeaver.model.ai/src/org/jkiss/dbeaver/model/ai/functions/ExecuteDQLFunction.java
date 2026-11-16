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
package org.jkiss.dbeaver.model.ai.functions;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.functions.heuristics.DQLScopeHeuristics;
import org.jkiss.dbeaver.model.ai.functions.heuristics.SQLScopeHeuristics;
import org.jkiss.dbeaver.model.ai.utils.AIDataExportUtils;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class ExecuteDQLFunction extends ExecuteSQLFunction {
    private static final SQLScopeHeuristics HEURISTICS = new DQLScopeHeuristics();
    public static final String MAX_ROWS_PARAM = "maxRows";
    public static final String FORMAT_PARAM = "format";

    @NotNull
    @Override
    protected SQLQueryCategory getQueryCategory() {
        return SQLQueryCategory.DQL;
    }

    @NotNull
    @Override
    protected SQLScopeHeuristics getScopeHeuristics() {
        return HEURISTICS;
    }

    @NotNull
    @Override
    protected String getResultValue(@NotNull String output, @NotNull DBCStatistics statistics) {
        return output;
    }

    @Override
    protected int getMaxRows(@NotNull Map<String, Object> parameters) {
        return CommonUtils.toInt(parameters.get(MAX_ROWS_PARAM), 10);
    }

    @NotNull
    @Override
    protected DataTransferProcessorDescriptor getResultProcessor(@NotNull Map<String, Object> parameters) throws DBException {
        return AIDataExportUtils.getProcessor(CommonUtils.toString(parameters.get(FORMAT_PARAM), "csv"));
    }
}
