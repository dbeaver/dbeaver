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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.net.URI;
import java.nio.file.Path;

public class IncludedScriptFileEditorInput extends FileStoreEditorInput {

    private final Path includedScriptFile;
    private DatabaseEditorContext databaseEditorContext;

    public IncludedScriptFileEditorInput(@NotNull IFileStore incIFile, @NotNull Path includedScriptFile) {
        super(incIFile);
        this.includedScriptFile = includedScriptFile;
    }

    @Override
    @NotNull
    public URI getURI() {
        return includedScriptFile.toUri();
    }

    @NotNull
    public Path getIncludedScriptFile() {
        return includedScriptFile;
    }

    @Nullable
    public DatabaseEditorContext getDatabaseEditorContext() {
        return databaseEditorContext;
    }

    public void setDatabaseEditorContext(@Nullable DatabaseEditorContext databaseEditorContext) {
        this.databaseEditorContext = databaseEditorContext;
    }

    @Override
    public String toString() {
        return "IncludedScriptFileEditorInput (" + includedScriptFile + ")";
    }
}
