/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.meta.Property;

import java.util.List;
import java.util.Map;

/**
 * Resource manager API
 */
public class RMResource implements RMObject {
    private String name;
    private boolean folder;
    private long length;

    private List<RMResourceChange> changes;
    private Map<String, Object> properties;

    public RMResource() {

    }

    public RMResource(String name) {
        this.name = name;
    }

    public RMResource(String name, boolean folder, long length) {
        this.name = name;
        this.folder = folder;
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getPath() {
        return name;
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

    public List<RMResourceChange> getChanges() {
        return changes;
    }

    public void setChanges(List<RMResourceChange> changes) {
        this.changes = changes;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

}
