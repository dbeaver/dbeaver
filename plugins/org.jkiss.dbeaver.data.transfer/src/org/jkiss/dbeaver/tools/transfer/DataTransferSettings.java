/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataTransferSettings
 */
public class DataTransferSettings {

    private static final Log log = Log.getLog(DataTransferSettings.class);

    public static final int DEFAULT_THREADS_NUM = 1;

    private List<DataTransferPipe> dataPipes;

    private DataTransferNodeDescriptor producer;
    private DataTransferNodeDescriptor consumer;

    private final Map<DataTransferNodeDescriptor, IDataTransferSettings> nodeSettings = new HashMap<>();


    private DataTransferProcessorDescriptor processor;
    private Map<DataTransferProcessorDescriptor, Map<Object, Object>> processorPropsHistory = new HashMap<>();
    private boolean producerProcessor;

    private final IDataTransferProducer[] initProducers;
    private final @Nullable IDataTransferConsumer[] initConsumers;
    private final List<DBSObject> initObjects = new ArrayList<>();

    private boolean consumerOptional;
    private boolean producerOptional;
    private int maxJobCount = DEFAULT_THREADS_NUM;

    private transient int curPipeNum = 0;

    private boolean showFinalMessage = true;

    public DataTransferSettings(@Nullable IDataTransferProducer[] producers, @Nullable IDataTransferConsumer[] consumers) {
        this.initProducers = producers;
        this.initConsumers = consumers;
        dataPipes = new ArrayList<>();

        DataTransferRegistry registry = DataTransferRegistry.getInstance();

        if (!ArrayUtils.isEmpty(producers) && !ArrayUtils.isEmpty(consumers)) {
            if (producers.length != consumers.length) {
                throw new IllegalArgumentException("Producers number must match consumers number");
            }
            // Make pipes
            for (int i = 0; i < producers.length; i++) {
                if (producers[i].getDatabaseObject() != null) initObjects.add(producers[i].getDatabaseObject());
                dataPipes.add(new DataTransferPipe(producers[i], consumers[i]));
            }
            consumerOptional = false;
        } else if (!ArrayUtils.isEmpty(producers)) {
            // Make pipes
            for (IDataTransferProducer source : producers) {
                if (source.getDatabaseObject() != null) initObjects.add(source.getDatabaseObject());
                dataPipes.add(new DataTransferPipe(source, null));
            }
            // Set default producer
            Class<? extends IDataTransferProducer> producerType = dataPipes.get(0).getProducer().getClass();
            DataTransferNodeDescriptor producerDesc = registry.getNodeByType(producerType);
            if (producerDesc != null) {
                selectProducer(producerDesc);
                consumerOptional = true;
            } else {
                DBWorkbench.getPlatformUI().showError("Can't find producer", "Can't find data propducer descriptor in registry");
            }
        } else if (!ArrayUtils.isEmpty(consumers)) {
            // Make pipes
            for (IDataTransferConsumer target : consumers) {
                if (target.getDatabaseObject() != null) initObjects.add(target.getDatabaseObject());
                dataPipes.add(new DataTransferPipe(null, target));
            }
            // Set default consumer
            Class<? extends IDataTransferConsumer> consumerType = dataPipes.get(0).getConsumer().getClass();
            DataTransferNodeDescriptor consumerDesc = registry.getNodeByType(consumerType);
            if (consumerDesc != null) {
                selectConsumer(consumerDesc, null, false);
                consumerOptional = false;
            } else {
                DBWorkbench.getPlatformUI().showError("Can't find producer", "Can't find data propducer descriptor in registry");
            }
            producerOptional = true;
        } else {
            throw new IllegalArgumentException("Producers or consumers must be specified");
        }

        if (!ArrayUtils.isEmpty(consumers)) {
            for (IDataTransferConsumer target : consumers) {
                DataTransferNodeDescriptor node = registry.getNodeByType(target.getClass());
                if (node != null) {
                    this.consumer = node;
                }
            }
        }
    }

    public boolean isConsumerOptional() {
        return consumerOptional;
    }

    public boolean isProducerOptional() {
        return producerOptional;
    }

    public IDataTransferNode[] getInitProducers() {
        return initProducers;
    }

    @Nullable
    public IDataTransferConsumer[] getInitConsumers() {
        return initConsumers;
    }

    public List<DBSObject> getSourceObjects() {
        return initObjects;
    }

    @Nullable
    public IDataTransferSettings getNodeSettings(DataTransferNodeDescriptor node) {
        IDataTransferSettings settings = nodeSettings.get(node);
        if (settings == null) {
            try {
                settings = node.createSettings();
            } catch (DBException e) {
                log.error(e);
                return null;
            }
            nodeSettings.put(node, settings);
        }
        return settings;
    }

    public Map<DataTransferProcessorDescriptor, Map<Object, Object>> getProcessorPropsHistory() {
        return processorPropsHistory;
    }

    public Map<Object, Object> getProcessorProperties() {
        if (processor == null) {
            throw new IllegalStateException("No processor selected");
        }
        return processorPropsHistory.get(processor);
    }

    public void setProcessorProperties(Map<Object, Object> properties) {
        if (processor == null) {
            throw new IllegalStateException("No processor selected");
        }
        processorPropsHistory.put(processor, properties);
    }


    public List<DataTransferPipe> getDataPipes() {
        return dataPipes;
    }

    public synchronized DataTransferPipe acquireDataPipe(DBRProgressMonitor monitor) {
        if (curPipeNum >= dataPipes.size()) {
            // End of transfer
            // Signal last pipe about it
            if (!dataPipes.isEmpty()) {
                dataPipes.get(dataPipes.size() - 1).getConsumer().finishTransfer(monitor, true);
            }
            return null;
        }

        DataTransferPipe result = dataPipes.get(curPipeNum);

        curPipeNum++;
        return result;
    }

    public DataTransferNodeDescriptor getProducer() {
        return producer;
    }

    public void setProducer(DataTransferNodeDescriptor producer) {
        this.producer = producer;
    }

    public DataTransferNodeDescriptor getConsumer() {
        return consumer;
    }

    public void setConsumer(DataTransferNodeDescriptor consumer) {
        this.consumer = consumer;
    }

    public DataTransferProcessorDescriptor getProcessor() {
        return processor;
    }

    public boolean isProducerProcessor() {
        return producerProcessor;
    }

    private void selectProducer(DataTransferNodeDescriptor producer) {
        this.producer = producer;
    }

    public void selectConsumer(DataTransferNodeDescriptor consumer, DataTransferProcessorDescriptor processor, boolean rewrite) {
        this.consumer = consumer;
        this.processor = processor;
        this.producerProcessor = false;
        if (consumer != null && processor != null) {
            if (!processorPropsHistory.containsKey(processor)) {
                processorPropsHistory.put(processor, new HashMap<>());
            }
        }
        // Configure pipes
        for (int i = 0; i < dataPipes.size(); i++) {
            DataTransferPipe pipe = dataPipes.get(i);
            if (!rewrite && pipe.getConsumer() != null) {
                continue;
            }
            if (consumer != null) {
                try {
                    IDataTransferConsumer consumerNode = (IDataTransferConsumer) consumer.createNode();
                    /*if (pipe.getProducer() != null) {
                        IDataTransferConsumer.TransferParameters parameters = new IDataTransferConsumer.TransferParameters(
                            processor != null && processor.isBinaryFormat(),
                            processor != null && processor.isHTMLFormat());
                        parameters.orderNumber = i;
                        parameters.totalConsumers = dataPipes.size();

                        consumerNode.initTransfer(
                            pipe.getProducer().getDatabaseObject(),
                            getNodeSettings(consumerNode),
                            parameters,
                            processor != null ? processor.getInstance() : null,
                            processor != null ? getProcessorProperties() : null);
                    }*/
                    pipe.setConsumer(consumerNode);
                } catch (DBException e) {
                    log.error(e);
                    pipe.setConsumer(null);
                }
            } else {
                pipe.setConsumer(null);
            }
        }
    }

    public void selectProducer(DataTransferNodeDescriptor producer, DataTransferProcessorDescriptor processor, boolean rewrite) {
        this.producer = producer;
        this.processor = processor;
        if (producer != null && processor != null) {
            this.producerProcessor = true;
            if (!processorPropsHistory.containsKey(processor)) {
                processorPropsHistory.put(processor, new HashMap<>());
            }
        }
        // Configure pipes
        for (DataTransferPipe pipe : dataPipes) {
            if (!rewrite && pipe.getProducer() != null) {
                continue;
            }
            if (producer != null) {
                try {
                    pipe.setProducer((IDataTransferProducer) producer.createNode());
                } catch (DBException e) {
                    log.error(e);
                    pipe.setProducer(null);
                }
            } else {
                pipe.setProducer(null);
            }
        }
    }

    public int getMaxJobCount() {
        return maxJobCount;
    }

    public void setMaxJobCount(int maxJobCount) {
        if (maxJobCount > 0) {
            this.maxJobCount = maxJobCount;
        }
    }

    public boolean isShowFinalMessage() {
        return showFinalMessage;
    }

    public void setShowFinalMessage(boolean showFinalMessage) {
        this.showFinalMessage = showFinalMessage;
    }


}
