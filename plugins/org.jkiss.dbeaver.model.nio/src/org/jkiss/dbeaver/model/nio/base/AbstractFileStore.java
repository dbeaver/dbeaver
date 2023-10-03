/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.nio.base;

import org.jkiss.code.NotNull;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileStoreAttributeView;

public abstract class AbstractFileStore extends FileStore {
    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        return null;
    }

    @Override
    public Object getAttribute(@NotNull String attribute) throws IOException {
        switch (attribute) {
            case "totalSpace":
                return getTotalSpace();
            case "usableSpace":
                return getUsableSpace();
            case "unallocatedSpace":
                return getUnallocatedSpace();
            default:
                throw new UnsupportedOperationException("does not support the given attribute");
        }
    }
}
