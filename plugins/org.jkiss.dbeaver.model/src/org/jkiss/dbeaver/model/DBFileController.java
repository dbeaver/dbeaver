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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

/**
 * Binary files controller.
 *
 * There are different file types.
 * - 'database-driver' is for driver jar files
 */
public interface DBFileController extends DBPObjectController {

    String TYPE_DATABASE_DRIVER = "libraries";
    String DATA_FOLDER = "data";

    byte[] loadFileData(@NotNull String fileType, @NotNull String filePath) throws DBException;

    void saveFileData(@NotNull String fileType, @NotNull String filePath, byte[] fileData) throws DBException;

    String[] listFiles(@NotNull String fileType, @NotNull String filePath) throws DBException;

    void deleteFile(@NotNull String fileType, @NotNull String filePath, boolean recursive) throws DBException;

}
