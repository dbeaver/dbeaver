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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.dpi.DPIElement;
import org.jkiss.dbeaver.model.dpi.DPIObject;

import java.util.Map;

/**
 * Configuration origin.
 * It can be local configuration or some cloud provider.
 */
@DPIObject
@DPIElement
public interface DBPDataSourceOrigin extends DBPObjectWithDetails<DBPDataSourceContainer> {

    /**
     * Origin type. Unique
     */
    @NotNull
    String getType();

    /**
     * Origin sub type
     */
    @Nullable
    String getSubType();

    @NotNull
    String getDisplayName();

    @Nullable
    DBPImage getIcon();

    boolean isDynamic();

    @NotNull
    Map<String, Object> getDataSourceConfiguration();

    @NotNull
    default String getFullType(){
        return getType() + (getSubType() == null ? "" : "." + getSubType());
    }

}
