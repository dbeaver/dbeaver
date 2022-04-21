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

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Resource manager API
 */
public class RMResource {
    private String name;
    private boolean folder;
    private String contentType;
    private Charset contentCharset;
    private long length;

    private List<RMResourceChange> changes;
    private Map<String, Object> properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Charset getContentCharset() {
        return contentCharset;
    }

    public void setContentCharset(Charset contentCharset) {
        this.contentCharset = contentCharset;
    }

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
