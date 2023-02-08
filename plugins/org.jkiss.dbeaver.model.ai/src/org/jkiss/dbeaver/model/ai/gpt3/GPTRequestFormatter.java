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
package org.jkiss.dbeaver.model.ai.gpt3;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class GPTRequestFormatter {

    private static final Log log = Log.getLog(GPTRequestFormatter.class);

    private static final int MAX_PROMPT_LENGTH = 7500; // 8000 -

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
        additionalMetadata.append("### ")
            .append(context.getDataSource().getSQLDialect().getDialectName())
            .append(" SQL tables, with their properties:\n#\n");
        String tail = "";
        if (executionContext != null && executionContext.getContextDefaults() != null) {
            DBSSchema defaultSchema = executionContext.getContextDefaults().getDefaultSchema();
            if (defaultSchema != null) {
                tail += "#\n# Current schema is " + defaultSchema.getName() + "\n";
            }
        }
        int maxRequestLength = MAX_PROMPT_LENGTH - additionalMetadata.length() - tail.length() - 20;

        additionalMetadata.append(generateObjectDescription(monitor, context, maxRequestLength));
        additionalMetadata.append(tail).append("#\n###").append(request.trim()).append("\nSELECT");
        return additionalMetadata.toString();
    }

    private static String generateObjectDescription(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject object,
        int maxRequestLength
    ) throws DBException {
        if (DBNUtils.getNodeByObject(monitor, object, false) == null) {
            // Skip hidden objects
            return "";
        }
        StringBuilder request = new StringBuilder();
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
            for (DBSObject child : ((DBSObjectContainer) object).getChildren(monitor)) {
                if (DBUtils.isSystemObject(child) || DBUtils.isHiddenObject(child) || child instanceof DBSTablePartition) {
                    continue;
                }
                String childText = generateObjectDescription(monitor, child, maxRequestLength);
                if (request.length() + childText.length() > maxRequestLength) {
                    log.debug("Trim GPT metadata prompt  at table '" + child.getName() + "' - too long request");
                    break;
                }
                request.append(childText);
                totalChildren++;
            }
        }
        return request.toString();
    }

    private GPTRequestFormatter() {

    }
}
