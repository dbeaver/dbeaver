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
package org.jkiss.dbeaver.registry.fs;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.fs.DBFFileSystemDescriptor;
import org.jkiss.dbeaver.model.fs.DBFFileSystemProvider;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * PlatformLanguageDescriptor
 */
public class FileSystemProviderDescriptor extends AbstractDescriptor implements DBFFileSystemDescriptor {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.fileSystem"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private final DBPImage icon;
    private final String schema;
    private final ObjectType implClass;
    private final String requiredAuth;

    private DBFFileSystemProvider instance;

    public FileSystemProviderDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.requiredAuth = CommonUtils.nullIfEmpty(config.getAttribute("requiredAuth"));
        this.icon = iconToImage(config.getAttribute("icon"));
        this.schema = config.getAttribute("schema");
        this.implClass = new ObjectType(config.getAttribute("class"));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DBPImage getIcon() {
        return icon;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    @NotNull
    public synchronized DBFFileSystemProvider getInstance() {
        if (instance == null) {
            try {
                instance = implClass.createInstance(DBFFileSystemProvider.class);
            } catch (DBException e) {
                throw new IllegalStateException("Error instantiating file system provider " + id, e);
            }
        }
        return instance;
    }


    @Override
    public String toString() {
        return getLabel();
    }

    @Nullable
    public String getRequiredAuth() {
        return requiredAuth;
    }
}
