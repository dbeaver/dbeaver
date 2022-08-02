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
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Resource manager API
 */
public class RMProject extends RMObject {

    public static final String PREFIX_GLOBAL = "g";
    public static final String PREFIX_SHARED = "s";
    public static final String PREFIX_USER = "u";
    public static final Type[] SHARED_PROJECTS = {Type.GLOBAL, Type.SHARED};

    public enum Type {
        GLOBAL(PREFIX_GLOBAL),
        SHARED(PREFIX_SHARED),
        USER(PREFIX_USER);

        private final String prefix;

        Type(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }
    private String id;
    private String description;
    private Type type;
    private OffsetDateTime createTime;
    private String creator;
    private Set<String> projectPermissions;

    public RMProject() {
    }

    public RMProject(
        String id,
        String name,
        String description,
        Type type,
        OffsetDateTime createTime,
        String creator,
        Set<String> projectPermissions
    ) {
        super(name);
        this.id = id;
        this.description = description;
        this.type = type;
        this.createTime = createTime;
        this.creator = creator;
        this.projectPermissions = projectPermissions;
    }

    public RMProject(String name) {
        super(name);
    }

    @Property
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Property
    public boolean isShared() {
        return ArrayUtils.contains(SHARED_PROJECTS, getType());
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Property
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public OffsetDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(OffsetDateTime createTime) {
        this.createTime = createTime;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RMProject && CommonUtils.equalObjects(id, ((RMProject) obj).id);
    }

    public void setProjectPermissions(Set<String> projectPermissions) {
        this.projectPermissions = projectPermissions;
    }

    public Set<String> getProjectPermissions() {
        return projectPermissions;
    }
}
