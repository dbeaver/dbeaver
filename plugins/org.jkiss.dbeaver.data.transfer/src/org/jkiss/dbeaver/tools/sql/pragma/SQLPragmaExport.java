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
package org.jkiss.dbeaver.tools.sql.pragma;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLPragmaHandler;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class SQLPragmaExport implements SQLPragmaHandler {
    // FIXME: Not a pleasant solution, but seems acceptable
    private static final String STREAM_CONSUMER_PREFIX = "stream_consumer:stream.";

    private static final Log log = Log.getLog(SQLPragmaExport.class);

    @Override
    public int processPragma(@NotNull DBRProgressMonitor monitor, @NotNull DBSDataContainer container, @NotNull Map<String, Object> params) throws DBException {
        final String type = JSONUtils.getString(params, "type");
        if (CommonUtils.isEmpty(type)) {
            throw new DBException("`type` attribute is mandatory");
        }

        final DataTransferProcessorDescriptor descriptor = DataTransferRegistry.getInstance().getProcessor(STREAM_CONSUMER_PREFIX + type);
        if (descriptor == null) {
            throw new DBException("Can't find processor of type '" + type + "'");
        }

        final IDataTransferProcessor processor = descriptor.getInstance();
        if (!(processor instanceof IStreamDataExporter)) {
            throw new DBException("Only stream exporters are supported");
        }

        final Map<String, Object> properties = new HashMap<>();
        for (DBPPropertyDescriptor property : descriptor.getProperties()) {
            properties.put(property.getId(), property.getDefaultValue());
        }
        for (Map.Entry<String, Object> property : JSONUtils.getObject(params, "props").entrySet()) {
            final DBPPropertyDescriptor propertyDescriptor = descriptor.getProperty(property.getKey());
            if (propertyDescriptor == null) {
                log.debug("Skipping unknown property " + property.getKey());
                continue;
            }
            properties.put(property.getKey(), PropertyDescriptor.convertString(CommonUtils.toString(property.getValue()), propertyDescriptor.getDataType()));
        }

        final StreamTransferConsumer consumer = new StreamTransferConsumer();
        final StreamConsumerSettings consumerSettings = new StreamConsumerSettings();

        // TODO: Find a way to pass those settings in
        consumerSettings.setOutputClipboard(true);
        consumerSettings.setLobExtractType(StreamConsumerSettings.LobExtractType.SKIP);

        consumer.initTransfer(
            container,
            consumerSettings,
            new IDataTransferConsumer.TransferParameters(),
            (IStreamDataExporter) processor,
            properties
        );

        final DatabaseTransferProducer producer = new DatabaseTransferProducer(container, null);
        final DatabaseProducerSettings producerSettings = new DatabaseProducerSettings();

        producerSettings.setExtractType(DatabaseProducerSettings.ExtractType.SINGLE_QUERY);
        producerSettings.setQueryRowCount(false);

        producer.transferData(monitor, consumer, processor, producerSettings, null);
        consumer.finishTransfer(monitor, false);
        consumer.finishTransfer(monitor, true);

        return RESULT_POP_PRAGMA | RESULT_SKIP_QUERY;
    }
}
