/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.tools.transfer.ui.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * DataTransferProcessorDescriptor
 */
public class DataTransferPageDescriptor extends AbstractDescriptor
{
    private final String id;
    private final boolean producerSelector;
    private final boolean consumerSelector;
    private final String producerType;
    private final String consumerType;
    private final DataTransferPageType pageType;
    private final ObjectType pageClass;

    DataTransferPageDescriptor(IConfigurationElement config)
    {
        super(config);
        this.id = config.getAttribute("id");
        this.pageClass = new ObjectType(config.getAttribute("class"));
        this.producerSelector = CommonUtils.toBoolean(config.getAttribute("producerSelector"));
        this.consumerSelector = CommonUtils.toBoolean(config.getAttribute("consumerSelector"));
        this.producerType = config.getAttribute("producerType");
        this.consumerType = config.getAttribute("consumerType");
        String typeStr = config.getAttribute("type");
        if (CommonUtils.isEmpty(typeStr)) {
            pageType = DataTransferPageType.NORMAL;
        } else {
            pageType = DataTransferPageType.valueOf(typeStr.toUpperCase(Locale.ENGLISH));
        }
    }

    public String getId()
    {
        return id;
    }

    public ObjectType getPageClass() {
        return pageClass;
    }

    public boolean isProducerSelector() {
        return producerSelector;
    }

    public boolean isConsumerSelector() {
        return consumerSelector;
    }

    public String getProducerType() {
        return producerType;
    }

    public String getConsumerType() {
        return consumerType;
    }

    public DataTransferPageType getPageType() {
        return pageType;
    }

    @Override
    public String toString() {
        return id;
    }
}
