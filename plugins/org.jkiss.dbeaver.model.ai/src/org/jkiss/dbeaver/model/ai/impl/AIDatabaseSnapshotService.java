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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AISchemaGenerationOptions;
import org.jkiss.dbeaver.model.ai.AISchemaGenerator;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates textual DB snapshots that are small enough to fit into an AI prompt.
 */
public class AIDatabaseSnapshotService {

    private static final Log LOG = Log.getLog(AIDatabaseSnapshotService.class);
    private AISchemaGenerator schemaGenerator;

    public AIDatabaseSnapshotService() {
    }

    @Nullable
    public TokenBoundedStringBuilder createDbSnapshot(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIDatabaseContext aiDatabaseContext,
        @NotNull AISchemaGenerationOptions options
    ) throws DBException {
        schemaGenerator = AIAssistantRegistry.getInstance().getDescriptor().createSchemaGenerator();

        if (aiDatabaseContext == null) {
            return null;
        }

        Objects.requireNonNull(aiDatabaseContext.getScopeObject(), "Scope object is null");
        Objects.requireNonNull(aiDatabaseContext.getExecutionContext(), "Execution context is null");

        var prompt = new TokenBoundedStringBuilder(options.maxDbSnapshotTokens(), false);

        if (appendContext(monitor, aiDatabaseContext, options, prompt, true)) {
            return prompt;
        }

        // --- fall-back -----------------------------------------------------
        AISchemaGenerationOptions fallback = buildFallbackOptions(options);
        if (options.equals(fallback)) {        // nothing else we can exclude
            return prompt;
        }

        LOG.warn("Context description is too long, generating partial description");

        var partialPrompt = new TokenBoundedStringBuilder(options.maxDbSnapshotTokens(), true);
        appendContext(monitor, aiDatabaseContext, fallback, partialPrompt, false);
        return partialPrompt;
    }

    /**
     * Returns {@code true} when the entire context fits into the provided builder.
     */
    private boolean appendContext(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIDatabaseContext ctx,
        @NotNull AISchemaGenerationOptions options,
        @NotNull TokenBoundedStringBuilder out,
        boolean refreshCache
    ) throws DBException {

        if (ctx.getScope() == AIDatabaseScope.CUSTOM && ctx.getCustomEntities() != null) {
            List<DBSObject> entities = normalizeCustomEntities(ctx.getCustomEntities());
            if (refreshCache) {
                cacheStructuresIfNeeded(monitor, entities);
            }

            for (DBSObject entity : entities) {
                if (!appendObjectDescription(
                    monitor,
                    out,
                    entity,
                    ctx.getExecutionContext(),
                    options,
                    requiresFqn(entity, ctx.getExecutionContext()),
                    refreshCache
                )) {
                    return false;
                }
            }
            return true;
        }

        return appendObjectDescription(
            monitor,
            out,
            ctx.getScopeObject(),
            ctx.getExecutionContext(),
            options,
            false,
            refreshCache
        );
    }

    private boolean appendObjectDescription(
        @NotNull DBRProgressMonitor monitor,
        @NotNull TokenBoundedStringBuilder out,
        @NotNull DBSObject obj,
        @Nullable DBCExecutionContext execCtx,
        @NotNull AISchemaGenerationOptions options,
        boolean useFqn,
        boolean refreshCache
    ) throws DBException {
        if (monitor.isCanceled()) {
            throw new DBException("Snapshot generation was canceled");
        }

        if (shouldSkipObject(monitor, obj)) {          // ignore system or hidden objects
            return true;
        }

        if (obj instanceof DBSEntity entity) {
            String ddl = schemaGenerator.generateSchema(monitor, entity, execCtx, options, useFqn) + "\n";
            return out.append(ddl);
        }

        if (obj instanceof DBSObjectContainer container) {
            return appendContainerDDL(monitor, out, container, execCtx, options, refreshCache);
        }

        return true;    // nothing to append for other object types
    }

    private boolean appendContainerDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull TokenBoundedStringBuilder out,
        @NotNull DBSObjectContainer container,
        @Nullable DBCExecutionContext execCtx,
        @NotNull AISchemaGenerationOptions options,
        boolean refreshCache
    ) throws DBException {

        if (refreshCache) {
            container.cacheStructure(
                monitor,
                DBSObjectContainer.STRUCT_ENTITIES | DBSObjectContainer.STRUCT_ATTRIBUTES
            );
        }

        for (DBSObject child : container.getChildren(monitor)) {
            if (shouldSkipObject(monitor, child)) {
                continue;
            }
            if (!appendObjectDescription(
                monitor,
                out,
                child,
                execCtx,
                options,
                requiresFqn(child, execCtx),
                refreshCache
            )) {

                LOG.warn("Object description is too long, truncated at: " + child.getName());
                return false;
            }
        }
        return true;
    }

    private static boolean shouldSkipObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject obj
    ) {
        return DBUtils.isSystemObject(obj)
            || DBUtils.isHiddenObject(obj)
            || obj instanceof DBSTablePartition
            || DBNUtils.getNodeByObject(monitor, obj, false) == null;
    }

    private static boolean requiresFqn(
        @NotNull DBSObject obj,
        @Nullable DBCExecutionContext ctx
    ) {
        if (ctx == null || ctx.getContextDefaults() == null) {
            return false;
        }
        DBSObject parent = obj.getParentObject();
        DBCExecutionContextDefaults<?, ?> def = ctx.getContextDefaults();
        return parent != null
            && !(parent.equals(def.getDefaultCatalog()) || parent.equals(def.getDefaultSchema()));
    }

    private static AISchemaGenerationOptions buildFallbackOptions(AISchemaGenerationOptions original) {
        return original.toBuilder()
            .withSendObjectComment(false)
            .withSendColumnTypes(false)
            .withSendForeignKeys(false)
            .withSendConstraints(false)
            .build();
    }

    /**
     * Leaves only top-level objects and sorts them to get deterministic output.
     */
    @NotNull
    private static List<DBSObject> normalizeCustomEntities(@NotNull List<DBSObject> entities) {
        Set<DBSObject> unique = new HashSet<>(entities);

        return unique.stream()
            .filter(o -> Stream.iterate(o.getParentObject(), Objects::nonNull, DBSObject::getParentObject)
                .noneMatch(unique::contains))
            .sorted(Comparator.comparing(DBPNamedObject::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    /**
     * Pre-caches container structures if many entities belong to the same container.
     */
    private static void cacheStructuresIfNeeded(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<DBSObject> entities
    ) {
        entities.stream()
            .filter(DBSEntity.class::isInstance)
            .map(o -> (DBSObjectContainer) o.getParentObject())
            .collect(Collectors.groupingBy(c -> c, Collectors.counting()))
            .forEach((container, count) -> {
                if (count > 1) { // avoid unnecessary caching
                    try {
                        container.cacheStructure(
                            monitor,
                            DBSObjectContainer.STRUCT_ENTITIES | DBSObjectContainer.STRUCT_ATTRIBUTES
                        );
                    } catch (DBException e) {
                        LOG.error("Failed to cache structure for " + container.getName(), e);
                    }
                }
            });
    }

    /**
     * Simple {@link StringBuilder} that stops accepting data once the specified
     * token limit (converted to characters) is reached.
     */
    public static class TokenBoundedStringBuilder {
        private static final int SAFE_MARGIN_TOKENS = 20;

        private final StringBuilder sb = new StringBuilder();
        private final int maxChars;
        private final boolean isTruncated;

        TokenBoundedStringBuilder(int maxTokens, boolean isTruncated) {
            this.maxChars = (maxTokens - SAFE_MARGIN_TOKENS) * DummyTokenCounter.TOKEN_TO_CHAR_RATIO;
            this.isTruncated = isTruncated;
        }

        boolean append(@NotNull CharSequence chunk) {
            if (sb.length() + chunk.length() > maxChars) {
                return false;
            }
            sb.append(chunk);
            return true;
        }

        boolean isTruncated() {
            return isTruncated;
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
