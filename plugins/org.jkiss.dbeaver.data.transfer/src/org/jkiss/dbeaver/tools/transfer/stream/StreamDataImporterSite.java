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
package org.jkiss.dbeaver.tools.transfer.stream;

import java.util.Map;

public class StreamDataImporterSite implements IStreamDataImporterSite {

    private StreamProducerSettings settings;
    private StreamEntityMapping entityMapping;
    private Map<String, Object> processorProperties;

    StreamDataImporterSite(StreamProducerSettings settings, StreamEntityMapping entityMapping, Map<String, Object> processorProperties) {
        this.settings = settings;
        this.entityMapping = entityMapping;
        this.processorProperties = processorProperties;
    }

    @Override
    public StreamProducerSettings getSettings() {
        return settings;
    }

    @Override
    public StreamEntityMapping getSourceObject() {
        return entityMapping;
    }

    @Override
    public Map<String, Object> getProcessorProperties() {
        return processorProperties;
    }

}
