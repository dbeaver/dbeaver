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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

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

            if (attribute.getMappingType() == StreamMappingType.keep) {
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
                readAttributes(monitor);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Data Transfer", "Error reading attributes from " + getName());
            } finally {
                monitor.done();
            }
        }
        return attributes;
    }

    private void readAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (source instanceof DBSEntity) {
            final DBSEntity entity = (DBSEntity) source;

            for (DBSEntityAttribute attribute : CommonUtils.safeList(entity.getAttributes(monitor))) {
                if (!DBUtils.isPseudoAttribute(attribute) && !DBUtils.isHiddenObject(attribute)) {
                    attributes.add(new StreamMappingAttribute(this, attribute, StreamMappingType.unspecified));
                }
            }
        } else {
            // TODO: What about dynamic queries?
            throw new DBException("Unsupported source object: " + getName());
        }
    }
}

