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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.utils.ArrayUtils;

/**
 * Abstract resource
 */
public abstract class RMObject implements DBPNamedObject {

    private RMResource[] children;
    private String name;

    public RMObject() {
    }

    public RMObject(String name) {
        this.name = name;
    }

    public abstract boolean isFolder();

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public RMResource[] getChildren() {
        return children;
    }

    public void setChildren(@NotNull RMResource[] resources) {
        this.children = resources;
    }

    @Nullable
    public RMResource getChild(@NotNull String name) {
        if (children != null) {
            for (RMResource child : children) {
                if (child.getName().equals(name)) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Updates resource for child object.
     * Used to refresh RM cache.
     * Should be used only by internal procedures
     */
    public void updateChild(@NotNull String name, @NotNull RMResource resource) {
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                RMResource child = children[i];
                if (child.getName().equals(name)) {
                    children[i] = resource;
                    break;
                }
            }
        }
    }

    public void addChild(RMResource child) {
        if (children == null) {
            children = new RMResource[] { child };
        } else {
            children = ArrayUtils.add(RMResource.class, children, child);
        }
    }

    public void removeChild(RMResource child) {
        if (children.length == 1 && children[0] == child) {
            children = null;
        } else {
            children = ArrayUtils.remove(RMResource.class, children, child);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

}
