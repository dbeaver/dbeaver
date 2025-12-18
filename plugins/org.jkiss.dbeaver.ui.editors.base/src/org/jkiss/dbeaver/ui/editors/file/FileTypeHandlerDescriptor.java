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

package org.jkiss.dbeaver.ui.editors.file;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DataFormatterDescriptor
 */
public class FileTypeHandlerDescriptor extends AbstractDescriptor {
    private static final Log log = Log.getLog(FileTypeHandlerDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.fileTypeHandler"; //$NON-NLS-1$

    private final String id;
    private final ObjectType handlerType;
    private final int order;
    private final boolean supportsRemote;
    private final boolean isDatabaseHandler;
    private final Extension[] extensions;

    public FileTypeHandlerDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.handlerType = new ObjectType(config.getAttribute("class"));
        this.supportsRemote = CommonUtils.toBoolean(config.getAttribute("remote"), true);
        this.isDatabaseHandler = CommonUtils.toBoolean(config.getAttribute("databaseHandler"), true);
        this.order = CommonUtils.toInt(config.getAttribute("order"));
        List<Extension> params = new ArrayList<>();
        for (IConfigurationElement ex : config.getChildren("extension")) {
            params.add(new Extension(ex, this));
        }
        this.extensions = params.toArray(new Extension[0]);
    }

    public String getId() {
        return id;
    }

    public Extension[] getExtensions() {
        return extensions;
    }

    public boolean supportsRemoteFiles() {
        return supportsRemote;
    }

    public boolean isDatabaseHandler() {
        return isDatabaseHandler;
    }

    public int getOrder() {
        return order;
    }

    public IFileTypeHandler createHandler() throws ReflectiveOperationException {
        Class<? extends IFileTypeHandler> clazz = handlerType.getImplClass(IFileTypeHandler.class);
        return clazz.getConstructor().newInstance();
    }

    public class Extension {
        private final FileTypeHandlerDescriptor descriptor;
        private final String id;
        private final String[] extensions;
        private final DBPImage icon;
        private final boolean supportsImport;

        Extension(@NotNull IConfigurationElement config, @NotNull FileTypeHandlerDescriptor descriptor) {
            this.descriptor = descriptor;
            this.id = config.getAttribute("id");
            this.extensions = CommonUtils.notEmpty(config.getAttribute("extensions")).split(",");
            this.icon = iconToImage(config.getAttribute("icon"));
            this.supportsImport = CommonUtils.toBoolean(config.getAttribute("supportsImport"), false);
        }

        @NotNull
        public FileTypeHandlerDescriptor getDescriptor() {
            return descriptor;
        }

        @Nullable
        public String getId() {
            return id;
        }

        @Nullable
        public DBPImage getIcon() {
            return icon;
        }

        @NotNull
        public String[] getExtensions() {
            return extensions;
        }

        public boolean isSupportsImport() {
            return supportsImport;
        }
    }

}
