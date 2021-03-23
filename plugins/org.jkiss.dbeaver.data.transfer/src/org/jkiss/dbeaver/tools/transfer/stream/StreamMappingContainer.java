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

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StreamMappingContainer implements DBPNamedObject, DBPImageProvider {
    private final DBSDataContainer source;
    private final List<StreamMappingAttribute> attributes;

    public StreamMappingContainer(@NotNull DBSDataContainer source) {
        this.source = source;
        this.attributes = new ArrayList<>();
    }

    public StreamMappingContainer(@NotNull StreamMappingContainer other) {
        this.source = other.source;
        this.attributes = new ArrayList<>();
        for (StreamMappingAttribute attribute : other.attributes) {
            this.attributes.add(new StreamMappingAttribute(this, attribute));
        }
    }

    public boolean isComplete() {
        boolean valid = false;

        for (StreamMappingAttribute attribute : attributes) {
            if (attribute.getMappingType() == StreamMappingType.unspecified) {
                return false;
            }

            if (attribute.getMappingType() == StreamMappingType.export) {
                valid = true;
            }
        }

        return valid;
    }

    @NotNull
    @Override
    public DBPImage getObjectImage() {
        return DBIcon.TREE_TABLE;
    }

    @NotNull
    @Override
    public String getName() {
        return DBUtils.getObjectFullName(source, DBPEvaluationContext.UI);
    }

    @NotNull
    public DBSDataContainer getSource() {
        return source;
    }

    @Nullable
    public StreamMappingAttribute getAttribute(@NotNull DBSAttributeBase sourceAttribute) {
        for (StreamMappingAttribute mappingAttribute : attributes) {
            if (mappingAttribute.getAttribute().getName().equals(sourceAttribute.getName())) {
                return mappingAttribute;
            }
        }

        return null;
    }

    @NotNull
    public List<StreamMappingAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) {
        if (attributes.isEmpty()) {
            try {
                monitor.beginTask("Load attributes from '" + getName() + "'", 1);
                for (DBSAttributeBase attribute : DTUtils.getAttributes(monitor, source, this)) {
                    attributes.add(new StreamMappingAttribute(this, attribute, StreamMappingType.unspecified));
                }
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(
                    DTMessages.stream_transfer_consumer_title_attributes_read_failed,
                    NLS.bind(DTMessages.stream_transfer_consumer_message_cannot_get_attributes_from, getName()),
                    e
                );
            } finally {
                monitor.done();
            }
        }
        return attributes;
    }

    public void loadSettings(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> containerSettings) {
        final Map<String, Object> attributes = JSONUtils.getObject(containerSettings, "attributes");
        for (StreamMappingAttribute attribute : getAttributes(monitor)) {
            final Map<String, Object> attributeSettings = JSONUtils.getObjectOrNull(attributes, attribute.getName());
            if (attributeSettings != null) {
                attribute.loadSettings(monitor, attributeSettings);
            }
        }
    }

    public void saveSettings(@NotNull Map<String, Object> containerSettings) {
        final Map<String, Object> attributesSettings = new LinkedHashMap<>();
        for (StreamMappingAttribute attribute : attributes) {
            final Map<String, Object> attributeSettings = new LinkedHashMap<>();
            attribute.saveSettings(attributeSettings);
            attributesSettings.put(attribute.getName(), attributeSettings);
        }
        containerSettings.put("attributes", attributesSettings);
    }
}

