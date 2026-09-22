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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * One file backing a Mimer SQL databank {@link MimerDatabankShadow shadow} - a read-only leaf so
 * the shadow node can be expanded to show which physical file(s) it uses (handy after a
 * "Switch Shadow To Master", which swaps the master's and shadow's files). Synthesised from the
 * owning shadow's already-loaded {@code EXT_SHADOWS.FILE_NAME}/{@code IS_ONLINE} - no extra query.
 * <p>
 * DbVisualizer's Mimer profile models a shadow as single-file, and so does this: a shadow of a
 * multi-file databank would only surface its first file here.
 *
 * @author Mimer Information Technology
 */
public class MimerShadowFile implements DBSObject {

    private final MimerDatabankShadow shadow;
    private final String fileName;
    private final boolean online;

    MimerShadowFile(@NotNull MimerDatabankShadow shadow, @Nullable String fileName, boolean online) {
        this.shadow = shadow;
        this.fileName = fileName;
        this.online = online;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return fileName == null ? "" : fileName;
    }

    @Property(viewable = true, order = 2)
    public boolean isOnline() {
        return online;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return shadow;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return shadow.getDataSource();
    }
}
