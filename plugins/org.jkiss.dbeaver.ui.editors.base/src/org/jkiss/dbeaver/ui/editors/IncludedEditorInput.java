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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.part.FileEditorInput;

import java.net.URI;
import java.nio.file.Path;

public class IncludedEditorInput extends FileEditorInput {

    private final Path incFile;
    private DatabaseEditorContext databaseEditorContext;

    private IncludedEditorInput(IFile incIFile, Path incFile) {
        super(incIFile);
        this.incFile = incFile;
    }

    public static IncludedEditorInput of(Path incFile) {
        return new IncludedEditorInput(getFile(incFile), incFile);
    }

    private static IFile getFile(Path pathToFile) {
        return ResourcesPlugin.getWorkspace().getRoot()
            .getFileForLocation(org.eclipse.core.runtime.Path.fromOSString(pathToFile.toString()));
    }

    @Override
    public URI getURI() {
        return incFile.toUri();
    }

    public Path getIncFile() {
        return incFile;
    }

    public DatabaseEditorContext getDatabaseEditorContext() {
        return databaseEditorContext;
    }

    public void setDatabaseEditorContext(DatabaseEditorContext databaseEditorContext) {
        this.databaseEditorContext = databaseEditorContext;
    }
}
