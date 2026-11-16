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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataKind;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * SQL dialect features used exclusively by the semantic analyzer (SQLQueryModelRecognizer)
 * and/or LSMAnalyzerParameters.
 * <p>
 * Members of this class are not used by any other part of the codebase outside the
 * semantic analysis and LSM parsing infrastructure.
 * <p>
 * Dialects that need custom behaviour create a private nested subclass overriding
 * the required methods and return it from {@link SQLDialect#getDialectFeatures()}.
 */
public abstract class SQLDialectFeatures {

    /**
     * Keywords that modify a data type declaration, in uppercase.
     */
    @NotNull
    public Set<String> getTypeModifierKeywords() {
        return Set.of("PRECISION", "VARYING", "LARGE", "OBJECT", "RAW");
    }

    /**
     * Keywords that modify a routine parameter signature, including parameter directions, in uppercase.
     */
    @NotNull
    public Set<String> getSignatureModifierKeywords() {
        return Set.of("IN", "OUT", "INOUT");
    }

    /**
     * Returns the visibility scope of projection aliases within a query.
     */
    @NotNull
    public EnumSet<ProjectionAliasVisibilityScope> getProjectionAliasVisibilityScope() {
        return EnumSet.of(
            ProjectionAliasVisibilityScope.WHERE,
            ProjectionAliasVisibilityScope.GROUP_BY,
            ProjectionAliasVisibilityScope.HAVING,
            ProjectionAliasVisibilityScope.ORDER_BY
        );
    }

    /**
     * Returns the policy for resolving conflicts between procedural variables and column names.
     */
    @NotNull
    public ProceduralVariableConflictPolicy getProceduralVariableConflictPolicy() {
        return ProceduralVariableConflictPolicy.UNRESOLVED;
    }

    /**
     * Returns the procedural syntax family for this dialect.
     */
    @NotNull
    public ProceduralSyntaxFamily getProceduralSyntaxFamily() {
        return ProceduralSyntaxFamily.GENERIC;
    }

    /**
     * Returns the trailing SELECT INTO support mode for this dialect.
     */
    @NotNull
    public TrailingSelectIntoSupport getTrailingSelectIntoSupport() {
        return TrailingSelectIntoSupport.NONE;
    }

    /**
     * Whether RETURNS TABLE column names are visible as variables in the routine body.
     */
    public boolean areRoutineTableResultColumnsBodyVisible() {
        return false;
    }

    /**
     * A capability must belong to exactly one namespace; a multi-capability binding can be indexed in several.
     */
    @NotNull
    public List<LocalVariableNamespace> getLocalVariableNamespaces() {
        return List.of(new LocalVariableNamespace(EnumSet.allOf(LocalVariableCapability.class)));
    }

    /**
     * Returns the visibility of a local variable of the given kind.
     */
    @NotNull
    public LocalVariableVisibility getLocalVariableVisibility(@NotNull LocalVariableKind kind) {
        return LocalVariableVisibility.LEXICAL;
    }

    /**
     * Returns the shadowing policy for a local variable of the given kind.
     */
    @NotNull
    public LocalVariableShadowing getLocalVariableShadowing(@NotNull LocalVariableKind kind) {
        return LocalVariableShadowing.ALLOW_OUTER;
    }

    /**
     * Returns global variable information for this dialect, or empty array if none.
     */
    @NotNull
    public GlobalVariableInfo[] getGlobalVariables() {
        return GlobalVariableInfo.EMPTY_ARRAY;
    }

    // ------------------------------------------------------------------------
    // Default implementation for dialects needing no overrides
    // ------------------------------------------------------------------------

    public static final class Default extends SQLDialectFeatures {
        private static final Default INSTANCE = new Default();

        public static Default getInstance() {
            return INSTANCE;
        }

        private Default() {
        }
    }

    // ------------------------------------------------------------------------
    // Nested types
    // ------------------------------------------------------------------------

    /**
     * Capabilities of an entity introduced into a procedural scope, independent of its declaration syntax.
     */
    public enum LocalVariableCapability { VALUE, FIELDS, ROWS, CURSOR }

    /**
     * Capabilities of an entity introduced into a procedural scope.
     */
    public enum LocalVariableKind {
        VALUE(true, false, false, false),
        ROW_VALUE(true, true, false, false),
        ROW_REFERENCE(false, true, false, false),
        ROWSET(false, false, true, false),
        CURSOR(false, false, false, true),
        CURSOR_VALUE(true, false, false, true);

        public final boolean canBeValue;
        public final boolean canAccessFields;
        public final boolean canBeRowsSource;
        public final boolean canBeCursor;
        private final Set<LocalVariableCapability> capabilities;

        LocalVariableKind(boolean canBeValue, boolean canAccessFields, boolean canBeRowsSource, boolean canBeCursor) {
            this.canBeValue = canBeValue;
            this.canAccessFields = canAccessFields;
            this.canBeRowsSource = canBeRowsSource;
            this.canBeCursor = canBeCursor;
            EnumSet<LocalVariableCapability> result = EnumSet.noneOf(LocalVariableCapability.class);
            if (canBeValue) {
                result.add(LocalVariableCapability.VALUE);
            }
            if (canAccessFields) {
                result.add(LocalVariableCapability.FIELDS);
            }
            if (canBeRowsSource) {
                result.add(LocalVariableCapability.ROWS);
            }
            if (canBeCursor) {
                result.add(LocalVariableCapability.CURSOR);
            }
            this.capabilities = Set.copyOf(result);
        }

        @NotNull
        public Set<LocalVariableCapability> capabilities() {
            return this.capabilities;
        }
    }

    /**
     * One conflict namespace and lookup index for the indicated operation capabilities.
     */
    public record LocalVariableNamespace(Set<LocalVariableCapability> capabilities) {
        public LocalVariableNamespace {
            if (capabilities.isEmpty()) {
                throw new IllegalArgumentException("Invalid local binding capabilities");
            }
            capabilities = Set.copyOf(capabilities);
        }
    }

    /**
     * Local declarations never have the script tracker's BATCH or SESSION lifetime.
     */
    public enum LocalVariableVisibility { LEXICAL, PROGRAM }

    public enum LocalVariableShadowing { ALLOW_OUTER, REJECT_VISIBLE }

    public enum ProjectionAliasVisibilityScope {
        WHERE,
        HAVING,
        GROUP_BY,
        ORDER_BY
    }

    public enum ProceduralVariableConflictPolicy {
        UNRESOLVED,
        PREFER_VARIABLE,
        PREFER_COLUMN
    }

    public enum ProceduralSyntaxFamily {
        GENERIC,
        TSQL,
        FIREBIRD_PSQL
    }

    public enum TrailingSelectIntoSupport {
        NONE,
        PROCEDURAL,
        ANY
    }

    public record GlobalVariableInfo(String name, String description, DBPDataKind type) {
        public static final GlobalVariableInfo[] EMPTY_ARRAY = new GlobalVariableInfo[0];
    }
}
