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
package org.jkiss.dbeaver.model.datadam.sync.project.sync;

import com.google.gson.Gson;
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

public final class DDProjectSyncBindingStore {

    private static final Log log = Log.getLog(DDProjectSyncBindingStore.class);
    private static final String BINDING_FILE_NAME = "datadam-sync.json";
    private static final Gson GSON = JSONUtils.PRETTY_GSON;
    @NotNull
    private final Path bindingFile;

    DDProjectSyncBindingStore(@NotNull Path bindingFile) {
        this.bindingFile = bindingFile.toAbsolutePath().normalize();
    }

    /** Canonical lookup key; resolving it does not create the metadata directory. */
    @NotNull
    public static Path getBindingFile(@NotNull DBPProject project) {
        return project.getMetadataFolder(false).resolve(BINDING_FILE_NAME).toAbsolutePath().normalize();
    }

    @Nullable
    DDProjectSyncLocalBinding load() throws DBException {
        if (!Files.isRegularFile(bindingFile)) {
            return null;
        }
        try {
            DDProjectSyncLocalBinding binding = GSON.fromJson(Files.readString(bindingFile), DDProjectSyncLocalBinding.class);
            validate(binding);
            return binding;
        } catch (IOException | RuntimeException e) {
            throw new DBException("Error reading DataDam project binding from " + bindingFile, e);
        }
    }

    void save(@NotNull DDProjectSyncLocalBinding binding) throws DBException {
        validate(binding);
        Path temporary = null;
        try {
            Files.createDirectories(bindingFile.getParent());
            temporary = Files.createTempFile(bindingFile.getParent(), BINDING_FILE_NAME + ".", ".tmp");
            Files.writeString(temporary, GSON.toJson(binding));
            try {
                Files.move(temporary, bindingFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, bindingFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DBException("Error writing DataDam project binding to " + bindingFile, e);
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

    void delete() throws DBException {
        try {
            Files.deleteIfExists(bindingFile);
        } catch (IOException e) {
            throw new DBException("Error deleting DataDam project binding " + bindingFile, e);
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

}
