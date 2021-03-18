/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class StreamMappingAttribute implements DBPNamedObject, DBPImageProvider {
    private final StreamMappingContainer container;
    private final DBSAttributeBase attribute;
    private StreamMappingType mappingType;

    public StreamMappingAttribute(@NotNull StreamMappingContainer container, @NotNull DBSAttributeBase attribute, @NotNull StreamMappingType mappingType) {
        this.container = container;
        this.attribute = attribute;
        this.mappingType = mappingType;
    }

    public StreamMappingAttribute(@NotNull StreamMappingContainer container, @NotNull StreamMappingAttribute other) {
        this.container = container;
        this.attribute = other.attribute;
        this.mappingType = other.mappingType;
    }

    @NotNull
    @Override
    public String getName() {
        return DBUtils.getObjectFullName(attribute, DBPEvaluationContext.UI);
    }

    @NotNull
    @Override
    public DBPImage getObjectImage() {
        return DBValueFormatting.getObjectImage(attribute);
    }

    @NotNull
    public StreamMappingContainer getContainer() {
        return container;
    }

    @NotNull
    public DBSAttributeBase getAttribute() {
        return attribute;
    }

    @NotNull
    public StreamMappingType getMappingType() {
        return mappingType;
    }

    public void setMappingType(@NotNull StreamMappingType mappingType) {
        this.mappingType = mappingType;
    }

    public void loadSettings(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> attributeSettings) {
        final String type = JSONUtils.getString(attributeSettings, "mappingType");
        if (CommonUtils.isNotEmpty(type)) {
            this.mappingType = CommonUtils.valueOf(StreamMappingType.class, type, StreamMappingType.unspecified);
        }
    }

    public void saveSettings(@NotNull Map<String, Object> attributeSettings) {
        attributeSettings.put("mappingType", mappingType.name());
    }
}
