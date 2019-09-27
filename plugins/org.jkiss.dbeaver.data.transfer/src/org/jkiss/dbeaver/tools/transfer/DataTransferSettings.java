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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

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

    private IDataTransferProducer[] initProducers;
    private @Nullable IDataTransferConsumer[] initConsumers;
    private final List<DBSObject> initObjects = new ArrayList<>();

    private boolean consumerOptional;
    private boolean producerOptional;
    private int maxJobCount = DEFAULT_THREADS_NUM;

    private transient int curPipeNum = 0;

    private boolean showFinalMessage = true;

    public DataTransferSettings(DBRRunnableContext runnableContext, @Nullable Collection<IDataTransferProducer> producers, @Nullable Collection<IDataTransferConsumer> consumers, Map<String, Object> configuration) {
        initializePipes(producers, consumers);
        loadConfiguration(runnableContext, configuration);
    }

    private void initializePipes(@Nullable Collection<IDataTransferProducer> producers, @Nullable Collection<IDataTransferConsumer> consumers) {
        this.initProducers = producers == null ? null : producers.toArray(new IDataTransferProducer[0]);
        this.initConsumers = consumers == null ? null : consumers.toArray(new IDataTransferConsumer[0]);
        this.dataPipes = new ArrayList<>();
        this.initObjects.clear();
        this.consumerOptional = true;
        this.producerOptional = true;

        DataTransferRegistry registry = DataTransferRegistry.getInstance();

        if (!ArrayUtils.isEmpty(initProducers) && !ArrayUtils.isEmpty(initConsumers)) {
            // Both producers and consumers specified
            // Processor belongs to non-database nodes anyway
            if (initProducers.length != initConsumers.length) {
                throw new IllegalArgumentException("Producers number must match consumers number");
            }
            // Make pipes
            for (int i = 0; i < initProducers.length; i++) {
                if (initProducers[i].getDatabaseObject() != null) initObjects.add(initProducers[i].getDatabaseObject());
                dataPipes.add(new DataTransferPipe(initProducers[i], initConsumers[i]));
            }
            consumerOptional = initProducers[0] instanceof IDataTransferNodePrimary;
            producerOptional = initConsumers[0] instanceof IDataTransferNodePrimary;
        } else if (!ArrayUtils.isEmpty(initProducers)) {
            // Make pipes
            for (IDataTransferProducer source : initProducers) {
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
        } else if (!ArrayUtils.isEmpty(initConsumers)) {
            // Make pipes
            for (IDataTransferConsumer target : initConsumers) {
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
            consumerOptional = true;
            producerOptional = true;
        }

        if (!ArrayUtils.isEmpty(initConsumers)) {
            for (IDataTransferConsumer target : initConsumers) {
                DataTransferNodeDescriptor node = registry.getNodeByType(target.getClass());
                if (node != null) {
                    this.consumer = node;
                }
            }
        }
    }

    public DataTransferSettings(DBRRunnableContext runnableContext, DBTTask task) {
        this(
            runnableContext,
            getNodesFromLocation(runnableContext, task, "producers", IDataTransferProducer.class),
            getNodesFromLocation(runnableContext, task, "consumers", IDataTransferConsumer.class),
            JSONUtils.getObject(task.getProperties(), "configuration")
        );
    }

    private void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        this.setMaxJobCount(CommonUtils.toInt(config.get("maxJobCount"), DataTransferSettings.DEFAULT_THREADS_NUM));
        this.setShowFinalMessage(CommonUtils.getBoolean(config.get("showFinalMessage"), this.isShowFinalMessage()));

        DataTransferNodeDescriptor savedConsumer = null, savedProducer = null, processorNode = null;
        {
            {
                String consumerId = CommonUtils.toString(config.get("consumer"));
                if (!CommonUtils.isEmpty(consumerId)) {
                    DataTransferNodeDescriptor consumerNode = DataTransferRegistry.getInstance().getNodeById(consumerId);
                    if (consumerNode != null) {
                        this.setConsumer(savedConsumer = consumerNode);
                        if (this.isConsumerOptional()) {
                            processorNode = savedConsumer;
                        }
                    }
                }
            }
            {
                String producerId = CommonUtils.toString(config.get("producer"));
                if (!CommonUtils.isEmpty(producerId)) {
                    DataTransferNodeDescriptor producerNode = DataTransferRegistry.getInstance().getNodeById(producerId);
                    if (producerNode != null) {
                        this.setProducer(savedProducer = producerNode);
                        if (this.isProducerOptional()) {
                            processorNode = savedProducer;
                        }
                    }
                }
            }
        }

        DataTransferProcessorDescriptor savedProcessor = null;
        if (processorNode != null) {
            String processorId = CommonUtils.toString(config.get("processor"));
            if (!CommonUtils.isEmpty(processorId)) {
                savedProcessor = processorNode.getProcessor(processorId);
            }
        }
        if (this.consumerOptional && savedConsumer != null) {
            this.selectConsumer(savedConsumer, savedProcessor, false);
        }
        if (this.producerOptional && savedProducer != null) {
            this.selectProducer(savedProducer, savedProcessor, false);
        }

        // Load processor properties
        Map<String, Object> processorsSection = JSONUtils.getObject(config, "processors");
        {
            for (Map.Entry<String, Object> procIter : processorsSection.entrySet()) {
                Map<String, Object> procSection = (Map<String, Object>) procIter.getValue();
                String processorId = procIter.getKey();
                String nodeId = CommonUtils.toString(procSection.get("@node"));
                if (CommonUtils.isEmpty(nodeId)) {
                    // Legacy code support
                    int divPos = processorId.indexOf(':');
                    if (divPos != -1) {
                        nodeId = processorId.substring(0, divPos);
                        processorId = processorId.substring(divPos + 1);
                    }
                }
                String propNamesId = CommonUtils.toString(procSection.get("@propNames"));
                DataTransferNodeDescriptor node = DataTransferRegistry.getInstance().getNodeById(nodeId);
                if (node != null) {
                    Map<Object, Object> props = new HashMap<>();
                    DataTransferProcessorDescriptor nodeProcessor = node.getProcessor(processorId);
                    if (nodeProcessor != null) {
                        for (String prop : CommonUtils.splitString(propNamesId, ',')) {
                            props.put(prop, procSection.get(prop));
                        }
                        processorPropsHistory.put(nodeProcessor, props);
                    }
                }
            }
        }

        // Load nodes' settings (key is impl class simple name, value is descriptor)
        Map<String, DataTransferNodeDescriptor> nodeNames = new LinkedHashMap<>();
        if (producer != null) {
            nodeNames.put(producer.getNodeClass().getSimpleName(), producer);
        }
        if (consumer != null) {
            nodeNames.put(consumer.getNodeClass().getSimpleName(), consumer);
        }
        for (Map.Entry<String, DataTransferNodeDescriptor> node : nodeNames.entrySet()) {
            Map<String, Object> nodeSection = JSONUtils.getObject(config, node.getKey());
            IDataTransferSettings nodeSettings = this.getNodeSettings(node.getValue());
            if (nodeSettings != null) {
                nodeSettings.loadSettings(runnableContext, this, nodeSection);
            }
        }
    }

    public boolean isConsumerOptional() {
        return consumerOptional;
    }

    public boolean isProducerOptional() {
        return producerOptional;
    }

    public IDataTransferProducer[] getInitProducers() {
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

    public static void saveNodesLocation(Map<String, Object> state, Collection<IDataTransferNode> nodes, String nodeType) {
        if (nodes != null) {
            List<Map<String, Object>> inputObjects = new ArrayList<>();
            for (Object inputObject : nodes) {
                inputObjects.add(JSONUtils.serializeObject(inputObject));
            }
            state.put(nodeType, inputObjects);
        }
    }

    public static <T> List<T> getNodesFromLocation(@NotNull DBRRunnableContext runnableContext, DBTTask task, String nodeType, Class<T> nodeClass) {
        Map<String, Object> config = task.getProperties();
        List<T> result = new ArrayList<>();
        Object nodeList = config.get(nodeType);
        if (nodeList instanceof Collection) {
            for (Object nodeObj : (Collection)nodeList) {
                if (nodeObj instanceof Map) {
                    Object node = JSONUtils.deserializeObject(runnableContext, task, (Map<String, Object>) nodeObj);
                    if (nodeClass.isInstance(node)) {
                        result.add(nodeClass.cast(node));
                    }
                }
            }
        }
        return result;
    }

    public void clearDataPipes() {
        dataPipes.clear();
        initObjects.clear();
        initProducers = new IDataTransferProducer[0];
        initConsumers = new IDataTransferConsumer[0];
    }

    public void addDataPipe(IDataTransferProducer producer, IDataTransferConsumer consumer) {
        List<IDataTransferProducer> producers = null;
        List<IDataTransferConsumer> consumers = null;
        if (producer != null) {
            producers = new ArrayList<>();
            if (initProducers != null) Collections.addAll(producers, initProducers);
            producers.add(producer);
        }
        if (consumer != null) {
            consumers = new ArrayList<>();
            if (initConsumers != null) Collections.addAll(consumers, initConsumers);
            consumers.add(consumer);
        }
        initializePipes(producers, consumers);
    }

}
