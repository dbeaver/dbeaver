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
package org.jkiss.dbeaver.model.ai.formatter;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.GPTPreferences;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class GPTRequestFormatter {

    /**
     * Add completion metadata to request
     */
    public static String addDBMetadataToRequest(
        DBRProgressMonitor monitor,
        String request,
        DBCExecutionContext executionContext,
        DBSObjectContainer context
    ) throws DBException {
        if (context == null || context.getDataSource() == null || CommonUtils.isEmptyTrimmed(request)) {
            return request;
        }

        StringBuilder additionalMetadata = new StringBuilder();
        //additionalMetadata.append("Use SQL\n");
        additionalMetadata.append("### ")
            .append(context.getDataSource().getSQLDialect().getDialectName())
            .append(" SQL tables, with their properties:\n#\n");
        generateObjectDescription(monitor, additionalMetadata, context);
        if (executionContext != null && executionContext.getContextDefaults() != null) {
            DBSSchema defaultSchema = executionContext.getContextDefaults().getDefaultSchema();
            if (defaultSchema != null) {
                additionalMetadata.append("#\n# Current schema is ").append(defaultSchema.getName()).append("\n");
            }
        }
        additionalMetadata.append("#\n###").append(request.trim()).append("\nSELECT");
        return additionalMetadata.toString();
    }


    private static void generateObjectDescription(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StringBuilder request,
        @NotNull DBSObject object
    ) throws DBException {
        if (object instanceof DBSEntity) {
            request.append("# ").append(DBUtils.getQuotedIdentifier(object));
            List<? extends DBSEntityAttribute> attributes = ((DBSEntity) object).getAttributes(monitor);
            if (attributes != null) {
                request.append("(");
                boolean firstAttr = true;
                for (DBSEntityAttribute attribute : attributes) {
                    if (DBUtils.isHiddenObject(attribute)) {
                        continue;
                    }
                    if (!firstAttr) {
                        request.append(",");
                    }
                    firstAttr = false;
                    request.append(attribute.getName());
                }
                request.append(")");
            }
            request.append(";\n");
        } else if (object instanceof DBSObjectContainer) {
            monitor.subTask("Load cache of " + object.getName());
            ((DBSObjectContainer) object).cacheStructure(
                monitor,
                DBSObjectContainer.STRUCT_ENTITIES | DBSObjectContainer.STRUCT_ATTRIBUTES);
            int totalChildren = 0;
            int maxChildren = DBWorkbench.getPlatform().getPreferenceStore().getInt(GPTPreferences.GPT_MAX_TABLES);
            for (DBSObject child : ((DBSObjectContainer) object).getChildren(monitor)) {
                if (DBUtils.isSystemObject(child) || DBUtils.isHiddenObject(child) || child instanceof DBSTablePartition) {
                    continue;
                }
                totalChildren++;
                generateObjectDescription(monitor, request, child);
                if (maxChildren > 0 && totalChildren > maxChildren) {
                    break;
                }
            }
        }
    }

    private GPTRequestFormatter() {

    }
}
