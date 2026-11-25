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

/**
 * Defines how variables introduced by standalone SQL statements are tracked and exposed during semantic analysis.
 * This describes editor-side visibility and does not track actual statement execution or database session state.
 */
public enum SQLScriptVariableScope {
    /**
     * Variables are derived from the current document and ordered by statement position.
     * An operation affects only subsequent statements in the script.
     */
    SCRIPT,

    /**
     * Variables are derived from the current document and ordered by statement position.
     * An operation affects only subsequent statements in the same batch, and each batch separator starts a new scope.
     */
    BATCH,

    /**
     * Variables are treated as session-wide knowledge accumulated during the lifetime of the tracker.
     * An observed operation is visible at every document position, and the latest observed operation for the same
     * canonical name wins, regardless of statement position.
     * <p>
     * Observations survive document edits and removal because the corresponding statements might already have been
     * executed. This scope does not track actual execution or guarantee that a variable currently exists in the
     * database session. Clearing the tracker discards all observations.
     */
    SESSION
}
