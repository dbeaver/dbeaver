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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.io.InputStream;
import java.util.List;

/**
 * Represents an importer that can read multiple entities from a single stream.
 */
public interface IMultiStreamDataImporter extends IStreamDataImporter {
    /**
     * Extracts list of entities contained in the given stream.
     *
     * @param entityMapping original entity mapping
     * @param inputStream   stream to read entities from
     * @return list of entity mappings from the stream
     * @throws DBException on any DB or IO error
     */
    @NotNull
    List<StreamEntityMapping> readEntitiesInfo(
        @NotNull StreamEntityMapping entityMapping,
        @NotNull InputStream inputStream) throws DBException;
}
