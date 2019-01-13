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
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.wizard.IWizardPage;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferPageDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DataTransferSettings
 */
public class DataTransferSettings {

    private static final Log log = Log.getLog(DataTransferSettings.class);

    private static final int DEFAULT_THREADS_NUM = 1;

    public static class NodeSettings {
        DataTransferNodeDescriptor sourceNode;
        IDataTransferSettings settings;
        IWizardPage[] pages;

        private NodeSettings(DataTransferNodeDescriptor sourceNode, boolean consumerOptional, boolean producerOptional) throws DBException {
            this.sourceNode = sourceNode;
            this.settings = sourceNode.createSettings();
            this.pages = sourceNode.createWizardPages(consumerOptional, producerOptional);
        }

    }

    private List<DataTransferPipe> dataPipes;

    private DataTransferNodeDescriptor producer;
    private DataTransferNodeDescriptor consumer;

    private DataTransferProcessorDescriptor processor;
    private Map<DataTransferProcessorDescriptor, Map<Object, Object>> processorPropsHistory = new HashMap<>();

    private Map<Class, NodeSettings> nodeSettings = new LinkedHashMap<>();
    private List<DBSObject> initObjects = new ArrayList<>();

    private boolean consumerOptional;
    private boolean producerOptional;
    private int maxJobCount = DEFAULT_THREADS_NUM;

    private transient int curPipeNum = 0;

    private boolean showFinalMessage = true;

    public DataTransferSettings(@Nullable IDataTransferProducer[] producers, @Nullable IDataTransferConsumer[] consumers) {
        dataPipes = new ArrayList<>();
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
            DataTransferNodeDescriptor producerDesc = DataTransferRegistry.getInstance().getNodeByType(producerType);
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
            DataTransferNodeDescriptor consumerDesc = DataTransferRegistry.getInstance().getNodeByType(consumerType);
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

        Collection<DBSObject> objectTypes = getSourceObjects();
        List<DataTransferNodeDescriptor> nodes = new ArrayList<>();
        DataTransferRegistry registry = DataTransferRegistry.getInstance();
        if (ArrayUtils.isEmpty(producers)) {
            nodes.addAll(registry.getAvailableProducers(objectTypes));
        } else {
            for (IDataTransferProducer source : producers) {
                DataTransferNodeDescriptor node = registry.getNodeByType(source.getClass());
                if (node != null && !nodes.contains(node)) {
                    nodes.add(node);
                }
            }
        }
        if (ArrayUtils.isEmpty(consumers)) {
            nodes.addAll(registry.getAvailableConsumers(objectTypes));
        } else {
            for (IDataTransferConsumer target : consumers) {
                DataTransferNodeDescriptor node = registry.getNodeByType(target.getClass());
                if (node != null && !nodes.contains(node)) {
                    nodes.add(node);
                    this.consumer = node;
                }
            }
        }
        for (DataTransferNodeDescriptor node : nodes) {
            addNodeSettings(node);
        }
    }

    private void addNodeSettings(DataTransferNodeDescriptor node) {
        if (node == null) {
            return;
        }
        Class<? extends IDataTransferNode> nodeClass = node.getNodeClass();
        if (nodeSettings.containsKey(nodeClass)) {
            return;
        }
        try {
            nodeSettings.put(nodeClass, new NodeSettings(node, consumerOptional, producerOptional));
        } catch (DBException e) {
            log.error("Can't add node '" + node.getId() + "'", e);
        }
    }

    void addWizardPages(DataTransferWizard wizard) {
        for (NodeSettings nodeSettings : this.nodeSettings.values()) {
            if (nodeSettings.pages != null) {
                for (IWizardPage page : nodeSettings.pages) {
                    wizard.addPage(page);
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

    public boolean isPageValid(IWizardPage page) {
        return isPageValid(page, producer) || isPageValid(page, consumer);
    }

    private boolean isPageValid(IWizardPage page, DataTransferNodeDescriptor node) {
        NodeSettings nodeSettings = node == null ? null : this.nodeSettings.get(node.getNodeClass());
        if (nodeSettings != null && ArrayUtils.contains(nodeSettings.pages, page)) {
            // Check does page matches consumer and producer
            for (NodeSettings ns : this.nodeSettings.values()) {
                DataTransferPageDescriptor pd = ns.sourceNode.getPageDescriptor(page);
                if (pd != null) {
                    if (pd.getProducerType() != null && producer != null && !producer.getId().equals(pd.getProducerType())) {
                        // Producer doesn't match
                        return false;
                    }
                    if (pd.getConsumerType() != null && consumer != null && !consumer.getId().equals(pd.getConsumerType())) {
                        // Consumer doesn't match
                        return false;
                    }
                }
            }

            return true;
        }
        return false;
    }

    public List<DBSObject> getSourceObjects() {
        return initObjects;
    }

    public IDataTransferSettings getNodeSettings(IWizardPage page) {
        for (NodeSettings nodeSettings : this.nodeSettings.values()) {
            if (nodeSettings.pages != null) {
                for (IWizardPage nodePage : nodeSettings.pages) {
                    if (nodePage == page) {
                        return nodeSettings.settings;
                    }
                }
            }
        }
        return null;
    }

    public IDataTransferSettings getNodeSettings(IDataTransferNode node) {
        NodeSettings nodeSettings = this.nodeSettings.get(node.getClass());
        return nodeSettings == null ? null : nodeSettings.settings;
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

    public DataTransferNodeDescriptor getConsumer() {
        return consumer;
    }

    public DataTransferProcessorDescriptor getProcessor() {
        return processor;
    }

    private void selectProducer(DataTransferNodeDescriptor producer) {
        this.producer = producer;
    }

    void selectConsumer(DataTransferNodeDescriptor consumer, DataTransferProcessorDescriptor processor, boolean rewrite) {
        this.consumer = consumer;
        this.processor = processor;
        if (consumer != null && processor != null) {
            if (!processorPropsHistory.containsKey(processor)) {
                processorPropsHistory.put(processor, new HashMap<>());
            }
        }
        // Configure pipes
        for (DataTransferPipe pipe : dataPipes) {
            if (!rewrite && pipe.getConsumer() != null) {
                continue;
            }
            if (consumer != null) {
                try {
                    IDataTransferConsumer consumerNode = (IDataTransferConsumer) consumer.createNode();
                    if (pipe.getProducer() != null) {
                        consumerNode.initTransfer(
                            pipe.getProducer().getDatabaseObject(),
                            getNodeSettings(consumerNode),
                            processor != null && processor.isBinaryFormat(),
                            processor != null ? processor.getInstance() : null,
                            processor != null ? getProcessorProperties() : null);
                    }
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

    void selectProducer(DataTransferNodeDescriptor producer, DataTransferProcessorDescriptor processor, boolean rewrite) {
        this.producer = producer;
        this.processor = processor;
        if (producer != null && processor != null) {
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

    void loadFrom(IRunnableContext runnableContext, IDialogSettings dialogSettings) {
        try {
            maxJobCount = dialogSettings.getInt("maxJobCount");
        } catch (NumberFormatException e) {
            maxJobCount = DEFAULT_THREADS_NUM;
        }
        if (dialogSettings.get("showFinalMessage") != null) {
            showFinalMessage = dialogSettings.getBoolean("showFinalMessage");
        }

        if (consumerOptional || producerOptional) {
            DataTransferNodeDescriptor savedConsumer = null, savedProducer = null, savedNode = null;
            {
                if (consumerOptional) {
                    String consumerId = dialogSettings.get("consumer");
                    if (!CommonUtils.isEmpty(consumerId)) {
                        DataTransferNodeDescriptor consumerNode = DataTransferRegistry.getInstance().getNodeById(consumerId);
                        if (consumerNode != null) {
                            savedNode = savedConsumer = this.consumer = consumerNode;
                        }
                    }
                }
                if (producerOptional) {
                    String producerId = dialogSettings.get("producer");
                    if (!CommonUtils.isEmpty(producerId)) {
                        DataTransferNodeDescriptor producerNode = DataTransferRegistry.getInstance().getNodeById(producerId);
                        if (producerNode != null) {
                            savedNode = savedProducer = this.producer = producerNode;
                        }
                    }
                }
            }

            DataTransferProcessorDescriptor savedProcessor = null;
            if (savedNode != null) {
                String processorId = dialogSettings.get("processor");
                if (!CommonUtils.isEmpty(processorId)) {
                    savedProcessor = savedNode.getProcessor(processorId);
                }
            }
            if (savedConsumer != null) {
                selectConsumer(savedConsumer, savedProcessor, false);
            }
            if (savedProducer != null) {
                selectProducer(savedProducer, savedProcessor, false);
            }
        }

        // Load nodes' settings
        for (Map.Entry<Class, NodeSettings> entry : nodeSettings.entrySet()) {
            IDialogSettings nodeSection = DialogSettings.getOrCreateSection(dialogSettings, entry.getKey().getSimpleName());
            entry.getValue().settings.loadSettings(runnableContext, this, nodeSection);
        }
        IDialogSettings processorsSection = dialogSettings.getSection("processors");
        if (processorsSection != null) {
            for (IDialogSettings procSection : ArrayUtils.safeArray(processorsSection.getSections())) {
                String processorId = procSection.getName();
                String nodeId = procSection.get("@node");
                if (CommonUtils.isEmpty(nodeId)) {
                    // Legacy code support
                    int divPos = processorId.indexOf(':');
                    if (divPos != -1) {
                        nodeId = processorId.substring(0, divPos);
                        processorId = processorId.substring(divPos + 1);
                    }
                }
                String propNamesId = procSection.get("@propNames");
                DataTransferNodeDescriptor node = DataTransferRegistry.getInstance().getNodeById(nodeId);
                if (node != null) {
                    Map<Object, Object> props = new HashMap<>();
                    DataTransferProcessorDescriptor nodeProcessor = node.getProcessor(processorId);
                    if (nodeProcessor != null) {
                        for (String prop : CommonUtils.splitString(propNamesId, ',')) {
                            props.put(prop, procSection.get(prop));
                        }
                        processorPropsHistory.put(nodeProcessor, props);
                        NodeSettings nodeSettings = this.nodeSettings.get(node.getNodeClass());
                        if (nodeSettings != null) {

                        }
                    }
                }
            }
        }
    }

    void saveTo(IDialogSettings dialogSettings) {
        dialogSettings.put("maxJobCount", maxJobCount);
        dialogSettings.put("showFinalMessage", showFinalMessage);
        // Save nodes' settings
        for (Map.Entry<Class, NodeSettings> entry : nodeSettings.entrySet()) {
            IDialogSettings nodeSection = DialogSettings.getOrCreateSection(dialogSettings, entry.getKey().getSimpleName());
            entry.getValue().settings.saveSettings(nodeSection);
        }

        if (producer != null) {
            dialogSettings.put("producer", producer.getId());
        }
        if (consumer != null) {
            dialogSettings.put("consumer", consumer.getId());
        }
        if (processor != null) {
            dialogSettings.put("processor", processor.getId());
        }

        // Save processors' properties
        IDialogSettings processorsSection = dialogSettings.addNewSection("processors");
        for (DataTransferProcessorDescriptor procDescriptor : processorPropsHistory.keySet()) {
            IDialogSettings procSettings = processorsSection.addNewSection(procDescriptor.getFullId());
            Map<Object, Object> props = processorPropsHistory.get(procDescriptor);
            if (!CommonUtils.isEmpty(props)) {
                StringBuilder propNames = new StringBuilder();
                for (Map.Entry<Object, Object> prop : props.entrySet()) {
                    propNames.append(prop.getKey()).append(',');
                }
                procSettings.put("@propNames", propNames.toString());
                for (Map.Entry<Object, Object> prop : props.entrySet()) {
                    procSettings.put(CommonUtils.toString(prop.getKey()), CommonUtils.toString(prop.getValue()));
                }
            }
        }

    }

}
