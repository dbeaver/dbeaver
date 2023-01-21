/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferEventProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DataTransferEventProcessorDescriptor extends AbstractDescriptor {
    private final String id;
    private final ObjectType type;
    private final String label;
    private final String description;
    private final int order;
    private final Set<String> applicableNodeIds;

    protected DataTransferEventProcessorDescriptor(@NotNull IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.type = new ObjectType(config.getAttribute("class"));
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.order = CommonUtils.toInt(config.getAttribute("order"));
        this.applicableNodeIds = new HashSet<>(Arrays.asList(config.getAttribute("nodes").split(",")));
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public ObjectType getType() {
        return type;
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return order;
    }

    public boolean isApplicable(@NotNull String nodeId) {
        return applicableNodeIds.contains(nodeId);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends IDataTransferConsumer<?, ?>> IDataTransferEventProcessor<T> create() throws DBException {
        type.checkObjectClass(IDataTransferEventProcessor.class);
        try {
            return type
                .getObjectClass(IDataTransferEventProcessor.class)
                .getDeclaredConstructor()
                .newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create event processor", e);
        }
    }
}
