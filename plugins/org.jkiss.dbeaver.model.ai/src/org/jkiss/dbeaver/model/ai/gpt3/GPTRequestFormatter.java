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
import org.jkiss.dbeaver.model.ai.completion.DAICompletionRequest;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionScope;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

public class GPTRequestFormatter {

    private static final Log log = Log.getLog(GPTRequestFormatter.class);

    private static final int MAX_PROMPT_LENGTH = 7500; // 8000 -
    private static final boolean SUPPORTS_ATTRS = true;
    private static final boolean SUPPORTS_FKS = false;

    /**
     * Add completion metadata to request
     */
    public static String addDBMetadataToRequest(
        DBRProgressMonitor monitor,
        DAICompletionRequest request,
        DBCExecutionContext executionContext,
        DBSObjectContainer mainObject
    ) throws DBException {
        if (mainObject == null || mainObject.getDataSource() == null || CommonUtils.isEmptyTrimmed(request.getPromptText())) {
            throw new DBException("Invalid completion request");
        }

        StringBuilder additionalMetadata = new StringBuilder();
        additionalMetadata.append("### ")
            .append(mainObject.getDataSource().getSQLDialect().getDialectName())
            .append(" SQL tables, with their properties:\n#\n");
        String tail = "";
        if (executionContext != null && executionContext.getContextDefaults() != null) {
            DBSSchema defaultSchema = executionContext.getContextDefaults().getDefaultSchema();
            if (defaultSchema != null) {
                tail += "#\n# Current schema is " + defaultSchema.getName() + "\n";
            }
        }
        int maxRequestLength = MAX_PROMPT_LENGTH - additionalMetadata.length() - tail.length() - 20;

        if (request.getScope() != DAICompletionScope.CUSTOM) {
            additionalMetadata.append(generateObjectDescription(monitor, request, mainObject, maxRequestLength));
        } else {
            for (DBSEntity entity : request.getCustomEntities()) {
                additionalMetadata.append(generateObjectDescription(monitor, request, entity, maxRequestLength));
            }
        }
        additionalMetadata.append(tail).append("#\n###").append(request.getPromptText().trim()).append("\nSELECT");
        return additionalMetadata.toString();
    }

    private static String generateObjectDescription(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request,
        @NotNull DBSObject object,
        int maxRequestLength
    ) throws DBException {
        if (DBNUtils.getNodeByObject(monitor, object, false) == null) {
            // Skip hidden objects
            return "";
        }
        StringBuilder description = new StringBuilder();
        if (object instanceof DBSEntity) {
            description.append("# ").append(DBUtils.getQuotedIdentifier(object));
            description.append("(");
            boolean firstAttr = true;
            if (SUPPORTS_ATTRS) {
                List<? extends DBSEntityAttribute> attributes = ((DBSEntity) object).getAttributes(monitor);
                if (attributes != null) {
                    for (DBSEntityAttribute attribute : attributes) {
                        if (DBUtils.isHiddenObject(attribute)) {
                            continue;
                        }
                        if (!firstAttr) description.append(",");
                        firstAttr = false;
                        description.append(attribute.getName());
                    }
                }
            }
            if (SUPPORTS_FKS) {
                // TBD
                Collection<? extends DBSEntityAssociation> associations = ((DBSEntity) object).getAssociations(monitor);
                if (associations != null) {
                    for (DBSEntityAssociation association : associations) {
                        if (association instanceof DBSEntityReferrer) {
                            DBSEntity refEntity = association.getAssociatedEntity();
                            List<? extends DBSEntityAttributeRef> refAttrs = ((DBSEntityReferrer) association).getAttributeReferences(monitor);
                            if (refEntity != null && !CommonUtils.isEmpty(refAttrs)) {
                                if (!firstAttr) description.append(",");
                                firstAttr = false;
                                description.append("FOREIGN KEY (");
                                boolean firstRA = true;
                                for (DBSEntityAttributeRef ar : refAttrs) {
                                    if (ar.getAttribute() != null) {
                                        if (!firstRA) description.append(",");
                                        firstRA = false;
                                        description.append(ar.getAttribute().getName());
                                    }
                                }
                                description.append(") REFERENCES ").append(refEntity.getName());
                            }
                        }
                    }
                }
            }
            description.append(");\n");
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
                String childText = generateObjectDescription(monitor, request, child, maxRequestLength);
                if (description.length() + childText.length() > maxRequestLength) {
                    log.debug("Trim GPT metadata prompt  at table '" + child.getName() + "' - too long request");
                    break;
                }
                description.append(childText);
                totalChildren++;
            }
        }
        return description.toString();
    }

    private GPTRequestFormatter() {

    }
}
