/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBFileController;

import java.nio.file.Path;

public class LocalFileController implements DBFileController {

    private final Path dataFolder;

    public LocalFileController(Path dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public byte[] loadFileData(@NotNull String filePath) throws DBException {
        return new byte[0];
    }

    @Override
    public void saveFileData(@NotNull String filePath, byte[] fileData) throws DBException {

    }

    @Override
    public String[] listFiles(@NotNull String filePath) throws DBException {
        return new String[0];
    }

    @Override
    public void deleteFile(@NotNull String filePath, boolean recursive) throws DBException {

    }
}
