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
package org.jkiss.dbeaver.model.rm;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.meta.Property;

import java.util.List;
import java.util.Map;

/**
 * Resource manager API
 */
public class RMResource extends RMObject {
    private boolean folder;
    private long length;
    private Long lastModified;

    private List<RMResourceChange> changes;
    private Map<String, Object> properties;

    public RMResource() {

    }

    public RMResource(String name) {
        super(name);
    }

    public RMResource(@NotNull String name, boolean folder, long length) {
        super(name);
        this.folder = folder;
        this.length = length;
    }

    @Override
    @Property
    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    @Property
    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    @Property
    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public List<RMResourceChange> getChanges() {
        return changes;
    }

    public void setChanges(List<RMResourceChange> changes) {
        this.changes = changes;
    }

    public Map<String, Object> getProperties() {
        return properties == null ? Map.of() : properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

}
