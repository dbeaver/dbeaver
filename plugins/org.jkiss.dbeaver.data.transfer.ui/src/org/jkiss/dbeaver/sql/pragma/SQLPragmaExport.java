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
package org.jkiss.dbeaver.sql.pragma;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLPragmaHandler;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.DataTransferState;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class SQLPragmaExport implements SQLPragmaHandler {
    private static final Log log = Log.getLog(SQLPragmaExport.class);

    private static final String PRODUCER_NODE_ID = "database_producer";
    private static final String CONSUMER_NODE_ID = "stream_consumer";
    private static final String PROCESSOR_ID_PREFIX = CONSUMER_NODE_ID + ":stream.";

    @Override
    public int processPragma(@NotNull DBRProgressMonitor monitor, @NotNull DBSDataContainer container, @NotNull Map<String, Object> parameters) throws DBException {
        final String type = JSONUtils.getString(parameters, "type");

        if (CommonUtils.isEmpty(type)) {
            throw new DBException("`type` attribute is mandatory");
        }

        final DataTransferRegistry registry = DataTransferRegistry.getInstance();
        final DataTransferNodeDescriptor producerNode = registry.getNodeById(PRODUCER_NODE_ID);
        final DataTransferNodeDescriptor consumerNode = registry.getNodeById(CONSUMER_NODE_ID);
        final DataTransferProcessorDescriptor processor = registry.getProcessor(PROCESSOR_ID_PREFIX + type);

        if (processor == null) {
            throw new DBException("Can't find processor of type '" + type + "'");
        }

        final DataTransferSettings settings = new DataTransferSettings(
            Collections.singleton(new DatabaseTransferProducer(container, null)),
            Collections.singleton(new StreamTransferConsumer()),
            Map.of(
                "producer", producerNode.getId(),
                "consumer", consumerNode.getId(),
                "processor", processor.getId(),
                "processors", Map.of(
                    processor.getFullId(), createProcessorSettings(processor, parameters)
                ),
                producerNode.getNodeClass().getSimpleName(), createProducerSettings(parameters),
                consumerNode.getNodeClass().getSimpleName(), createConsumerSettings(parameters)
            ),
            new DataTransferState(),
            true,
            true,
            false,
            false
        );

        UIUtils.asyncExec(() -> {
            final DataTransferWizard wizard = new DataTransferWizard(null, settings, true) {
                @Override
                protected boolean includePipesConfigurationPage() {
                    return false;
                }
            };

            new TaskConfigurationWizardDialog(UIUtils.getActiveWorkbenchWindow(), wizard, null).open();
        });

        return RESULT_CONSUME_PRAGMA | RESULT_CONSUME_QUERY;
    }

    @NotNull
    private static Map<String, Object> createProducerSettings(@NotNull Map<String, Object> parameters) {
        // TODO: Do we need to sanitize input data here?
        return JSONUtils.getObject(parameters, "producer");
    }

    @NotNull
    private static Map<String, Object> createConsumerSettings(@NotNull Map<String, Object> parameters) {
        // TODO: Do we need to sanitize input data here?
        return JSONUtils.getObject(parameters, "consumer");
    }

    @NotNull
    private static Map<String, Object> createProcessorSettings(@NotNull DataTransferProcessorDescriptor processor, @NotNull Map<String, Object> parameters) {
        final Map<String, Object> properties = new HashMap<>();
        final StringJoiner names = new StringJoiner(",");

        for (DBPPropertyDescriptor property : processor.getProperties()) {
            properties.put(property.getId(), property.getDefaultValue());
            names.add(property.getId());
        }

        for (Map.Entry<String, Object> property : JSONUtils.getObject(parameters, "processor").entrySet()) {
            final DBPPropertyDescriptor propertyDescriptor = processor.getProperty(property.getKey());

            if (propertyDescriptor == null) {
                log.debug("Skipping unknown property " + property.getKey());
                continue;
            }

            properties.put(property.getKey(), PropertyDescriptor.convertString(CommonUtils.toString(property.getValue()), propertyDescriptor.getDataType()));
        }

        properties.put("@propNames", names.toString());

        return properties;
    }
}
