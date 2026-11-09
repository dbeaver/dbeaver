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
package org.jkiss.dbeaver.model.app;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

/**
 * Transforms an archived text resource before it is written to an imported project.
 * Registered for an exact project-relative resource path through org.jkiss.dbeaver.projectImport.
 * The caller is responsible for decoding and encoding the resource contents.
 */
public interface DBPProjectImportTransformer {

    /**
     * Transforms resource contents to reflect the destination project names before the resource is written.
     * Implementations must preserve references to projects not included in the mapping and unrelated resource contents.
     *
     * @param monitor      progress monitor for reporting progress and checking cancellation
     * @param contents     text contents to transform
     * @param projectNames mapping from original to destination names of all projects selected for import
     * @return transformed contents, or the original contents when no changes are necessary
     * @throws DBException if the transformation fails or is canceled
     */
    @NotNull
    String transform(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String contents,
        @NotNull Map<String, String> projectNames
    ) throws DBException;
}
