/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.metadata;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionContext;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionMessage;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionScope;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class MetadataProcessor {
    public static final MetadataProcessor INSTANCE = new MetadataProcessor();
    private static final Log log = Log.getLog(MetadataProcessor.class);

    private static final boolean SUPPORTS_ATTRS = true;

    public String generateObjectDescription(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject object,
        @Nullable DBCExecutionContext context,
        @NotNull IAIFormatter formatter,
        int maxRequestLength,
        boolean useFullyQualifiedName
    ) throws DBException {
        if (DBNUtils.getNodeByObject(monitor, object, false) == null) {
            // Skip hidden objects
            return "";
        }
        StringBuilder description = new StringBuilder();
        if (object instanceof DBSEntity entity) {
            String name = useFullyQualifiedName && context != null ? DBUtils.getObjectFullName(
                context.getDataSource(),
                object,
                DBPEvaluationContext.DDL
            ) : DBUtils.getQuotedIdentifier(object);
            description.append('\n');
            formatter.addObjectDescriptionIfNeeded(description, object, monitor);
            if (object instanceof DBSTable table) {
                description.append(table.isView() ? "CREATE VIEW" : "CREATE TABLE");
            }
            description.append(" ").append(name).append("(");
            DBSEntityAttribute firstAttr = addPromptAttributes(monitor, entity, description, formatter);
            formatter.addExtraDescription(monitor, entity, description, firstAttr);
            description.append(");");
        } else if (object instanceof DBSObjectContainer objectContainer) {
            monitor.subTask("Load cache of " + object.getName());
            objectContainer.cacheStructure(
                monitor,
                DBSObjectContainer.STRUCT_ENTITIES | DBSObjectContainer.STRUCT_ATTRIBUTES);
            for (DBSObject child : objectContainer.getChildren(monitor)) {
                if (DBUtils.isSystemObject(child) || DBUtils.isHiddenObject(child) || child instanceof DBSTablePartition) {
                    continue;
                }
                String childText = generateObjectDescription(
                    monitor,
                    child,
                    context,
                    formatter,
                    maxRequestLength,
                    isRequiresFullyQualifiedName(child, context)
                );
                if (description.length() + childText.length() > maxRequestLength * 3) {
                    log.debug("Trim AI metadata prompt  at table '" + child.getName() + "' - too long request");
                    break;
                }
                description.append(childText);
            }
        }
        return description.toString();
    }

    /**
     * Creates a new message containing completion metadata for the request
     */
    @NotNull
    public DAICompletionMessage createMetadataMessage(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @Nullable DBSObjectContainer mainObject,
        @NotNull IAIFormatter formatter,
        @NotNull String instructions,
        int maxRequestTokens
    ) throws DBException {
        if (mainObject == null || mainObject.getDataSource() == null) {
            throw new DBException("Invalid completion request");
        }

        final DBCExecutionContext executionContext = context.getExecutionContext();
        final StringBuilder sb = new StringBuilder(instructions);
        final String extraInstructions = formatter.getExtraInstructions(monitor, mainObject, executionContext);
        if (CommonUtils.isNotEmpty(extraInstructions)) {
            sb.append(", ").append(extraInstructions);
        }

        sb.append("\nDialect is ").append(mainObject.getDataSource().getSQLDialect().getDialectName());

        if (executionContext.getContextDefaults() != null) {
            final DBSSchema defaultSchema = executionContext.getContextDefaults().getDefaultSchema();
            if (defaultSchema != null) {
                sb.append("\nCurrent schema is ").append(defaultSchema.getName());
            }
        }

        sb.append("\nSQL tables, with their properties are:");

        final int remainingRequestTokens = maxRequestTokens - sb.length() - 20;

        if (context.getScope() == DAICompletionScope.CUSTOM) {
            for (DBSEntity entity : context.getCustomEntities()) {
                sb.append(generateObjectDescription(
                    monitor,
                    entity,
                    executionContext,
                    formatter,
                    remainingRequestTokens,
                    isRequiresFullyQualifiedName(entity, executionContext)
                ));
            }
        } else {
            sb.append(
                generateObjectDescription(
                monitor,
                mainObject,
                executionContext,
                formatter,
                remainingRequestTokens,
                false
            ));
        }

        return new DAICompletionMessage(
            DAICompletionMessage.Role.SYSTEM,
            sb.toString()
        );
    }

    protected DBSEntityAttribute addPromptAttributes(
        DBRProgressMonitor monitor,
        DBSEntity entity,
        StringBuilder prompt,
        IAIFormatter formatter
    ) throws DBException {
        DBSEntityAttribute prevAttribute = null;
        if (SUPPORTS_ATTRS) {
            List<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
            if (attributes != null) {
                for (DBSEntityAttribute attribute : attributes) {
                    if (DBUtils.isHiddenObject(attribute)) {
                        continue;
                    }
                    if (prevAttribute != null) {
                        prompt.append(",");
                        formatter.addObjectDescriptionIfNeeded(prompt, prevAttribute, monitor);
                        //prompt.append("\n\t");
                    }
                    prompt.append(attribute.getName());
                    formatter.addColumnTypeIfNeeded(prompt, attribute, monitor);
                    prevAttribute = attribute;
                }
            }
        }
        return prevAttribute;
    }

    private boolean isRequiresFullyQualifiedName(@NotNull DBSObject object, @Nullable DBCExecutionContext context) {
        if (context == null || context.getContextDefaults() == null) {
            return false;
        }
        DBSObject parent = object.getParentObject();
        DBCExecutionContextDefaults<?,?> contextDefaults = context.getContextDefaults();
        return parent != null && !(parent.equals(contextDefaults.getDefaultCatalog())
            || parent.equals(contextDefaults.getDefaultSchema()));
    }

    private MetadataProcessor() {

    }
}
