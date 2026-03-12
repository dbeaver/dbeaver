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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AISchemaGenerator;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates textual DB snapshots that are small enough to fit into an AI prompt.
 */
public class AIDatabaseSnapshotService {

    private static final Log log = Log.getLog(AIDatabaseSnapshotService.class);
    private AISchemaGenerator schemaGenerator;

    public AIDatabaseSnapshotService() {
    }

    @Nullable
    public TokenBoundedStringBuilder createDbSnapshot(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIDatabaseContext aiDatabaseContext,
        int tokenBudget
    ) throws DBException {
        schemaGenerator = AIAssistantRegistry.getInstance().getDescriptor().createSchemaGenerator();

        if (aiDatabaseContext == null) {
            return null;
        }

        Objects.requireNonNull(aiDatabaseContext.getScopeObject(), "Scope object is null");
        Objects.requireNonNull(aiDatabaseContext.getExecutionContext(), "Execution context is null");

        var prompt = new TokenBoundedStringBuilder(tokenBudget, false);

        if (appendContext(monitor, aiDatabaseContext, prompt, true)) {
            return prompt;
        }

        log.debug("Context description is too long, generating partial description");

        var partialPrompt = new TokenBoundedStringBuilder(tokenBudget, true);
        appendContext(monitor, aiDatabaseContext, partialPrompt, false);
        return partialPrompt;
    }

    /**
     * Returns {@code true} when the entire context fits into the provided builder.
     */
    private boolean appendContext(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIDatabaseContext ctx,
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
                    ctx,
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
            ctx,
            false,
            refreshCache
        );
    }

    private boolean appendObjectDescription(
        @NotNull DBRProgressMonitor monitor,
        @NotNull TokenBoundedStringBuilder out,
        @NotNull DBSObject obj,
        @NotNull AIDatabaseContext databaseContext,
        boolean useFqn,
        boolean refreshCache
    ) throws DBException {
        if (monitor.isCanceled()) {
            throw new DBException("Snapshot generation was canceled");
        }

        if (AIUtils.isExcludableObject(monitor, obj)) {          // ignore system or hidden objects
            return true;
        }

        if (obj instanceof DBSEntity entity) {
            try {
                String ddl = schemaGenerator.generateSchema(monitor, databaseContext, entity, useFqn) + "\n";
                return out.append(ddl);
            } catch (DBException e) {
                log.warn("Failed to read metadata for entity '" + entity.getName() + "'", e);
                return true;
            }
        }

        if (obj instanceof DBSObjectContainer container) {
            container.cacheStructure(monitor, DBSObjectContainer.STRUCT_ALL);
            return appendContainerDDL(monitor, out, container, databaseContext, refreshCache);
        }

        return true;    // nothing to append for other object types
    }

    private boolean appendContainerDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull TokenBoundedStringBuilder out,
        @NotNull DBSObjectContainer container,
        @NotNull AIDatabaseContext dbContext,
        boolean refreshCache
    ) {
        if (refreshCache) {
            try {
                container.cacheStructure(
                    monitor,
                    DBSObjectContainer.STRUCT_ENTITIES | DBSObjectContainer.STRUCT_ATTRIBUTES
                );
            } catch (DBException e) {
                log.warn("Failed to cache for '" + container.getName() + "'. Proceeding.", e);
            }
        }

        try {
            Collection<? extends DBSObject> children = container.getChildren(monitor);
            if (children == null) {
                return true;
            }
            for (DBSObject child : children) {
                if (AIUtils.isExcludableObject(monitor, child)) {
                    continue;
                }
                try {
                    if (!appendObjectDescription(
                        monitor,
                        out,
                        child,
                        dbContext,
                        requiresFqn(child, dbContext.getExecutionContext()),
                        refreshCache
                    )) {
                        log.debug("Object description is too long, truncated at: " + child.getName());
                        return false;
                    }
                } catch (DBException e) {
                    log.warn(
                        "Failed to read metadata for child '" + child.getName()
                            + "' of container '" + container.getName() + "'",
                        e
                    );
                }
            }
        } catch (DBException e) {
            log.warn("Failed to children for '" + container.getName() + "'", e);
            return true;
        }

        return true;
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
                        log.error("Failed to cache structure for " + container.getName(), e);
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
        private boolean isTruncated;

        TokenBoundedStringBuilder(int maxTokens, boolean isTruncated) {
            this.maxChars = (maxTokens - SAFE_MARGIN_TOKENS) * DummyTokenCounter.TOKEN_TO_CHAR_RATIO;
            this.isTruncated = isTruncated;
        }

        boolean append(@NotNull CharSequence chunk) {
            if (sb.length() + chunk.length() > maxChars) {
                this.isTruncated = true;
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
