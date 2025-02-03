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
package org.jkiss.dbeaver.ui.editors.sql.scripts;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.file.IFileTypeHandler;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * SQL file handler
 */
public class SQLFileHandler implements IFileTypeHandler {

    @Override
    public void openFiles(
        @NotNull List<Path> fileList,
        @NotNull Map<String, String> parameters,
        @Nullable DBPDataSourceContainer dataSource
    ) {
        for (Path path : fileList) {
            File file = path.toFile();
            if (dataSource != null) {
                EditorUtils.setFileDataSource(file, new SQLNavigatorContext(dataSource));
            }
            EditorUtils.openExternalFileEditor(file, UIUtils.getActiveWorkbenchWindow());
        }
    }

}
