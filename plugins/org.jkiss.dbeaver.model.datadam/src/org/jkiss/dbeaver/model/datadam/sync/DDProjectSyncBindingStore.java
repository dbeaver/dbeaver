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
package org.jkiss.dbeaver.model.datadam.sync;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class DDProjectSyncBindingStore {

    private static final Log log = Log.getLog(DDProjectSyncBindingStore.class);

    private static final String BINDING_FILE_NAME = "datadam-sync.json";

    @Nullable
    DDProjectSyncLocalBinding load(@NotNull DBPProject project) throws DBException {
        Path file = getBindingFile(project, false);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            DDProjectSyncLocalBinding binding = JSONUtils.GSON.fromJson(Files.readString(file), DDProjectSyncLocalBinding.class);
            validate(binding);
            return binding;
        } catch (IOException | RuntimeException e) {
            throw new DBException("Error reading DataDam project binding from " + file, e);
        }
    }

    void save(@NotNull DBPProject project, @NotNull DDProjectSyncLocalBinding binding) throws DBException {
        validate(binding);
        Path file = getBindingFile(project, true);
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), BINDING_FILE_NAME + ".", ".tmp");
            Files.writeString(temporary, JSONUtils.GSON.toJson(binding));
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DBException("Error writing DataDam project binding to " + file, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    log.debug("Error deleting temporary DataDam project binding " + temporary, e);
                }
            }
        }
    }

    void delete(@NotNull DBPProject project) throws DBException {
        Path file = getBindingFile(project, false);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new DBException("Error deleting DataDam project binding " + file, e);
        }
    }

    private void validate(@Nullable DDProjectSyncLocalBinding binding) throws DBException {
        //for deserialization possible problems
        if (binding == null || binding.remoteProjectId() == null || binding.accountId() == null ||
            binding.lastSyncedRevision() == null || CommonUtils.isEmpty(binding.unitIds())) {
            throw new DBException("Invalid DataDam project binding");
        }
        for (String unitId : binding.unitIds()) {
            if (CommonUtils.isEmpty(unitId)) {
                throw new DBException("Invalid DataDam project binding");
            }
        }
    }

    @NotNull
    private Path getBindingFile(@NotNull DBPProject project, boolean createMetadataFolder) {
        return project.getMetadataFolder(createMetadataFolder).resolve(BINDING_FILE_NAME);
    }
}
