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

import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Set;

/**
 * Resource manager API
 */
public class RMProject extends RMObject {

    private static final RMProjectType[] SHARED_PROJECTS = {RMProjectType.GLOBAL, RMProjectType.SHARED};

    private String id;
    private String description;
    private RMProjectType type;
    private Long createTime;
    private String creator;
    private Set<String> projectPermissions;

    public RMProject() {
    }

    public RMProject(
        String id,
        String name,
        String description,
        RMProjectType type,
        Long createTime,
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

    public String getDisplayName() {
        switch (type) {
            case GLOBAL:
                return ModelMessages.project_shared_display_name;
            case USER:
                return ModelMessages.project_private_display_name;
            default:
                return getName();
        }
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
    public RMProjectType getType() {
        return type;
    }

    public void setType(RMProjectType type) {
        this.type = type;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
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
