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
package org.jkiss.dbeaver.model.tracking.sync.core;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.util.List;

/**
 * Remote storage of opaque values. Knows nothing about their content or encryption.
 */
public interface DDSyncTransport {

    @NotNull
    List<DDContainer> listContainers() throws DBException;

    @NotNull
    DDContainer createContainer(@NotNull String label) throws DBException;

    @NotNull
    List<DDRawEntry> load(@NotNull String containerId) throws DBException;

    void save(@NotNull String containerId, @NotNull String key, @NotNull byte[] value) throws DBException;
}
