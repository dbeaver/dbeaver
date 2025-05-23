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
package org.jkiss.dbeaver.model.ai.metadata;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionContext;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionScope;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;

import java.util.*;
import java.util.stream.Collectors;

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
            if (object instanceof DBSDataContainer dataContainer) {
                formatter.addDataSample(monitor, dataContainer, description);
            }

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
    public String describeContext(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @NotNull IAIFormatter formatter,
        int maxRequestTokens
    ) throws DBException {
        DBSObjectContainer mainObject = context.getScopeObject();

        if (mainObject == null || mainObject.getDataSource() == null) {
            throw new DBException("Invalid completion request");
        }

        final DBCExecutionContext executionContext = context.getExecutionContext();
        final StringBuilder sb = new StringBuilder();

        final int remainingRequestTokens = maxRequestTokens - sb.length() - 20;

        if (context.getScope() == DAICompletionScope.CUSTOM) {
            List<DBSObject> normalizeCustomEntities = normalizeCustomEntities(context.getCustomEntities());
            cacheStructuresForCustomEntities(monitor, normalizeCustomEntities);

            for (DBSObject entity : normalizeCustomEntities) {
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

        return sb.toString();
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

    /**
     * Normalizes the given list by removing a DBSObject if any of its ancestors
     * (database, schema, container, etc.) are already present in the same list.
     * The result therefore contains only the highest-level objects, with no
     * duplicates, ordered alphabetically by name.
     *
     * @param customEntities list that may contain databases, schemas, tables, etc.
     * @return normalized, alphabetically sorted list of top-level objects
     */
    private List<DBSObject> normalizeCustomEntities(@NotNull List<DBSObject> customEntities) {
        Set<DBSObject> input = new HashSet<>(customEntities);

        return input.stream()
            // skip the object if any ancestor is also present in the input
            .filter(obj -> {
                DBSObject parent = obj.getParentObject();
                while (parent != null) {
                    if (input.contains(parent)) {
                        return false;
                    }
                    parent = parent.getParentObject();
                }
                return true;
            })
            .sorted(Comparator.comparing(DBPNamedObject::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    /**
     * Caches for custom entities if there are multiple entities in the same container.
     * This is needed to avoid multiple calls to the same container.
     */
    private void cacheStructuresForCustomEntities(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<DBSObject> customEntities
    ) throws DBException {
        Set<Map.Entry<DBSObjectContainer, Long>> objectContainers = customEntities.stream()
            .filter(it -> it instanceof DBSEntity)
            .map(it -> (DBSObjectContainer) it.getParentObject())
            .collect(Collectors.groupingBy(it -> it, Collectors.counting()))
            .entrySet();

        for (Map.Entry<DBSObjectContainer, Long> entry : objectContainers) {
            if (entry.getValue() > 1) {
                entry.getKey().cacheStructure(
                    monitor,
                    DBSObjectContainer.STRUCT_ENTITIES | DBSObjectContainer.STRUCT_ATTRIBUTES
                );
            }
        }
    }
}
