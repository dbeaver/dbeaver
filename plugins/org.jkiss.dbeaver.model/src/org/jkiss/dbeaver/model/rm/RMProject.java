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
package org.jkiss.dbeaver.model.rm;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

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
    private String[] projectPermissions;
    private RMResourceType[] resourceTypes;

    public RMProject() {
    }

    public RMProject(
        String id,
        String name,
        String description,
        RMProjectType type,
        Long createTime,
        String creator,
        String[] projectPermissions
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Property(viewable = true, order = 1)
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

    public boolean isShared() {
        return ArrayUtils.contains(SHARED_PROJECTS, getType());
    }

    public boolean isGlobal() {
        return getType() == RMProjectType.GLOBAL;
    }

    @Property(viewable = true, order = 2)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Property(viewable = true, order = 3)
    public RMProjectType getType() {
        return type;
    }

    public void setType(RMProjectType type) {
        this.type = type;
    }

    @Property(viewable = true, valueRenderer = TimeRenderer.class, order = 10)
    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    @Property(viewable = true, order = 11)
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

    public String[] getProjectPermissions() {
        return projectPermissions;
    }

    public boolean hasProjectPermission(String permission) {
        return ArrayUtils.contains(projectPermissions, permission);
    }

    public void setProjectPermissions(String[] projectPermissions) {
        this.projectPermissions = projectPermissions;
    }

    public RMResourceType[] getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(RMResourceType[] resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public static class TimeRenderer implements IPropertyValueTransformer<RMProject, Object> {
        @Override
        public Object transform(RMProject object, Object value) throws IllegalArgumentException {
            if (!(value instanceof Long)) {
                return value;
            }
            return new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT).format(new Date((Long) value));
        }
    }
}
