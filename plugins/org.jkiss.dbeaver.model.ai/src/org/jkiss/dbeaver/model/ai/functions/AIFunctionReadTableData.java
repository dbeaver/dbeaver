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
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIFunction;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.impl.SampleDataProvider;
import org.jkiss.dbeaver.model.ai.utils.AIDataExportUtils;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class AIFunctionReadTableData implements AIFunction {

    public static final String PARAM_TABLE = "table";
    public static final String PARAM_LIMIT = "rowCount";
    public static final String PARAM_FORMAT = "format";

    @NotNull
    @Override
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull Map<String, Object> parameters
    ) throws DBException {
        String tableName = JSONUtils.getString(parameters, PARAM_TABLE);

        if (CommonUtils.isEmpty(tableName)) {
            throw new DBException("Table name parameter is missing");
        }

        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        int defaultRowsCount = preferenceStore.getInt(AIConstants.AI_SAMPLE_ROWS_COUNT);
        int maxRowsCount = preferenceStore.getInt(AIConstants.AI_MAX_SAMPLE_ROWS_COUNT);

        int requestedRowCount = CommonUtils.toInt(parameters.get(PARAM_LIMIT), defaultRowsCount);
        boolean reducedByUserLimit = requestedRowCount > maxRowsCount;
        int rowCount = requestedRowCount;
        if (reducedByUserLimit) {
            rowCount = maxRowsCount;
        }
        if (rowCount <= 0) {
            throw new DBException("Sample row count must be greater than zero");
        }
        DBSDataContainer table = AIUtils.findTableInContext(context, tableName);
        AIDatabaseContext databaseContext = context.getContext();
        if (databaseContext == null || !AIUtils.isObjectInScope(
            databaseContext, databaseContext.getExecutionContext(), table)) {
            throw new DBException("Table '" + tableName + "' is not in scope. Access restricted.");
        }
        DataTransferProcessorDescriptor processor = AIDataExportUtils.getProcessor(
            CommonUtils.toString(parameters.get(PARAM_FORMAT), "sql"));
        String result = new SampleDataProvider().getSampleData(context.getMonitor(), table, rowCount, processor);
        if (CommonUtils.isEmpty(result)) {
            result = "There is no data for table '" + tableName + "'.";
        }

        if (reducedByUserLimit) {
            result += " Requested " + requestedRowCount + " rows, but this was reduced to " + rowCount
                + " due to the user-configured maximum sample rows limit.";
        }

        return new AIFunctionResult(
            AIFunctionType.INFORMATION,
            "Table '" + tableName + " data is: " + result
        );
    }
}
