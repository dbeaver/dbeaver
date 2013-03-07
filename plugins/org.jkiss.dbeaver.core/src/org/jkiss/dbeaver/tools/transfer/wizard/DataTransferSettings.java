/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 * eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DataTransferSettings
 */
public class DataTransferSettings {

    static final Log log = LogFactory.getLog(DataTransferSettings.class);

    private static final int DEFAULT_THREADS_NUM = 1;

    public static class NodeSettings {
        DataTransferNodeDescriptor sourceNode;
        IDataTransferSettings settings;
        IWizardPage[] pages;

        private NodeSettings(DataTransferNodeDescriptor sourceNode) throws DBException
        {
            this.sourceNode = sourceNode;
            this.settings = sourceNode.createSettings();
            this.pages = sourceNode.createWizardPages();
        }

    }

    private List<DataTransferPipe> dataPipes;

    private DataTransferNodeDescriptor producer;
    private DataTransferNodeDescriptor consumer;

    private DataTransferProcessorDescriptor processor;
    private Map<DataTransferProcessorDescriptor, Map<Object,Object>> processorPropsHistory = new HashMap<DataTransferProcessorDescriptor, Map<Object, Object>>();

    private Map<Class, NodeSettings> nodeSettings = new LinkedHashMap<Class, NodeSettings>();

    private int maxJobCount = DEFAULT_THREADS_NUM;

    private transient int curPipeNum = 0;

    public DataTransferSettings(Collection<? extends IDataTransferProducer> sources)
    {
        this(sources, null);
    }

    public DataTransferSettings(Collection<? extends IDataTransferProducer> producers, Collection<? extends IDataTransferConsumer> consumers)
    {
        dataPipes = new ArrayList<DataTransferPipe>();
        if (!CommonUtils.isEmpty(producers)) {
            // Make pipes
            for (IDataTransferProducer source : producers) {
                dataPipes.add(new DataTransferPipe(source, null));
            }
            // Set default producer
            Class<? extends IDataTransferProducer> producerType = dataPipes.get(0).getProducer().getClass();
            DataTransferNodeDescriptor producerDesc = DBeaverCore.getInstance().getDataTransferRegistry().getNodeByType(producerType);
            if (producerDesc != null) {
                selectProducer(producerDesc);
            } else {
                UIUtils.showErrorDialog(null, "Can't find producer", "Can't find data propducer descriptor in registry");
            }
        } else if (!CommonUtils.isEmpty(consumers)) {
            // Make pipes
            for (IDataTransferConsumer target : consumers) {
                dataPipes.add(new DataTransferPipe(null, target));
            }
            // Set default consumer
            Class<? extends IDataTransferConsumer> consumerType = dataPipes.get(0).getConsumer().getClass();
            DataTransferNodeDescriptor consumerDesc = DBeaverCore.getInstance().getDataTransferRegistry().getNodeByType(consumerType);
            if (consumerDesc != null) {
                selectConsumer(consumerDesc, null);
            } else {
                UIUtils.showErrorDialog(null, "Can't find producer", "Can't find data propducer descriptor in registry");
            }
        } else {
            throw new IllegalArgumentException("Producers must match consumers or must be empty");
        }

        Collection<Class<?>> objectTypes = getObjectTypes();
        List<DataTransferNodeDescriptor> nodes = new ArrayList<DataTransferNodeDescriptor>();
        DataTransferRegistry registry = DBeaverCore.getInstance().getDataTransferRegistry();
        nodes.addAll(registry.getAvailableProducers(objectTypes));
        nodes.addAll(registry.getAvailableConsumers(objectTypes));
        for (DataTransferNodeDescriptor node : nodes) {
            addNodeSettings(node);
        }
    }

    private void addNodeSettings(DataTransferNodeDescriptor node)
    {
        if (node == null) {
            return;
        }
        Class<? extends IDataTransferNode> nodeClass = node.getNodeClass();
        if (nodeSettings.containsKey(nodeClass)) {
            return;
        }
        try {
            nodeSettings.put(nodeClass, new NodeSettings(node));
        } catch (DBException e) {
            log.error("Can't add node '" + node.getId() + "'", e);
        }
    }

    void addWizardPages(DataTransferWizard wizard)
    {
        for (NodeSettings nodeSettings : this.nodeSettings.values()) {
            if (nodeSettings.pages != null) {
                for (IWizardPage page : nodeSettings.pages) {
                    wizard.addPage(page);
                }
            }
        }
    }

    public boolean isPageValid(IWizardPage page)
    {
        return isPageValid(page, producer) || isPageValid(page, consumer);
    }

    private boolean isPageValid(IWizardPage page, DataTransferNodeDescriptor node)
    {
        NodeSettings nodeSettings = node == null ? null : this.nodeSettings.get(node.getNodeClass());
        return nodeSettings != null && CommonUtils.contains(nodeSettings.pages, page);
    }

    public Collection<Class<?>> getObjectTypes()
    {
        List<DataTransferPipe> dataPipes = getDataPipes();
        Set<Class<?>> objectTypes = new HashSet<Class<?>>();
        for (DataTransferPipe transferPipe : dataPipes) {
            if (transferPipe.getProducer() != null) {
                objectTypes.add(transferPipe.getProducer().getSourceObject().getClass());
            }
        }
        return objectTypes;
    }

    public IDataTransferSettings getNodeSettings(IWizardPage page)
    {
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

    public IDataTransferSettings getNodeSettings(IDataTransferNode node)
    {
        NodeSettings nodeSettings = this.nodeSettings.get(node.getClass());
        return nodeSettings == null ? null : nodeSettings.settings;
    }

    public Map<Object, Object> getProcessorProperties()
    {
        if (processor == null) {
            throw new IllegalStateException("No processor selected");
        }
        return processorPropsHistory.get(processor);
    }

    public void setProcessorProperties(Map<Object, Object> properties)
    {
        if (processor == null) {
            throw new IllegalStateException("No processor selected");
        }
        processorPropsHistory.put(processor, properties);
    }


    public List<DataTransferPipe> getDataPipes()
    {
        return dataPipes;
    }

    public synchronized DataTransferPipe acquireDataPipe()
    {
        if (curPipeNum >= dataPipes.size()) {
            // End of transfer
            // Signal last pipe about it
            if (!dataPipes.isEmpty()) {
                dataPipes.get(dataPipes.size() - 1).getConsumer().finishTransfer();
            }
            return null;
        }

        DataTransferPipe result = dataPipes.get(curPipeNum);

        curPipeNum++;
        return result;
    }

    public DataTransferNodeDescriptor getProducer()
    {
        return producer;
    }

    public DataTransferNodeDescriptor getConsumer()
    {
        return consumer;
    }

    public DataTransferProcessorDescriptor getProcessor()
    {
        return processor;
    }

    private void selectProducer(DataTransferNodeDescriptor producer)
    {
        this.producer = producer;
    }

    void selectConsumer(DataTransferNodeDescriptor consumer, DataTransferProcessorDescriptor processor)
    {
        this.consumer = consumer;
        this.processor = processor;
        if (consumer != null && processor != null) {
            if (!processorPropsHistory.containsKey(processor)) {
                processorPropsHistory.put(processor, new HashMap<Object, Object>());
            }
        }
        // Configure pipes
        for (DataTransferPipe pipe : dataPipes) {
            if (consumer != null) {
                try {
                    pipe.setConsumer((IDataTransferConsumer) consumer.createNode());
                } catch (DBException e) {
                    log.error(e);
                    pipe.setConsumer(null);
                }
            } else {
                pipe.setConsumer(null);
            }
        }
    }

    public int getMaxJobCount()
    {
        return maxJobCount;
    }

    public void setMaxJobCount(int maxJobCount)
    {
        if (maxJobCount > 0) {
            this.maxJobCount = maxJobCount;
        }
    }

    void loadFrom(IDialogSettings dialogSettings)
    {
        try {
            maxJobCount = dialogSettings.getInt("maxJobCount");
        } catch (NumberFormatException e) {
            maxJobCount = DEFAULT_THREADS_NUM;
        }
        String producerId = dialogSettings.get("producer");
        if (!CommonUtils.isEmpty(producerId)) {
            this.producer = DBeaverCore.getInstance().getDataTransferRegistry().getNodeById(producerId);
        }

        DataTransferNodeDescriptor savedConsumer = null;
        String consumerId = dialogSettings.get("consumer");
        if (!CommonUtils.isEmpty(consumerId)) {
            savedConsumer = DBeaverCore.getInstance().getDataTransferRegistry().getNodeById(consumerId);
        }

        DataTransferProcessorDescriptor savedProcessor = null;
        if (savedConsumer != null) {
            String processorId = dialogSettings.get("processor");
            if (!CommonUtils.isEmpty(processorId)) {
                savedProcessor = savedConsumer.getProcessor(processorId);
            }
        }
        if (savedConsumer != null) {
            selectConsumer(savedConsumer, savedProcessor);
        }

        // Load nodes' settings
        for (Map.Entry<Class, NodeSettings> entry : nodeSettings.entrySet()) {
            IDialogSettings nodeSection = dialogSettings.addNewSection(entry.getKey().getSimpleName());
            entry.getValue().settings.loadSettings(nodeSection);
        }
        IDialogSettings processorsSection = dialogSettings.getSection("processors");
        if (processorsSection != null) {
            for (IDialogSettings procSection : CommonUtils.safeArray(processorsSection.getSections())) {
                String processorId = procSection.getName();
                String nodeId = procSection.get("@node");
                String propNamesId = procSection.get("@propNames");
                DataTransferNodeDescriptor node = DBeaverCore.getInstance().getDataTransferRegistry().getNodeById(nodeId);
                if (node != null) {
                    Map<Object, Object> props = new HashMap<Object, Object>();
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

    void saveTo(IDialogSettings dialogSettings)
    {
        dialogSettings.put("maxJobCount", maxJobCount);
        // Save nodes' settings
        for (Map.Entry<Class, NodeSettings> entry : nodeSettings.entrySet()) {
            IDialogSettings nodeSection = dialogSettings.addNewSection(entry.getKey().getSimpleName());
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
        IDialogSettings processorsSection = DialogSettings.getOrCreateSection(dialogSettings, "processors");
        for (DataTransferProcessorDescriptor procDescriptor : processorPropsHistory.keySet()) {
            IDialogSettings procSettings = DialogSettings.getOrCreateSection(processorsSection, procDescriptor.getId());
            procSettings.put("@node", procDescriptor.getNode().getId());
            Map<Object, Object> props = processorPropsHistory.get(procDescriptor);
            if (props != null) {
                StringBuilder propNames = new StringBuilder();
                for (Map.Entry<Object,Object> prop : props.entrySet()) {
                    propNames.append(prop.getKey()).append(',');
                }
                procSettings.put("@propNames", propNames.toString());
                for (Map.Entry<Object,Object> prop : props.entrySet()) {
                    procSettings.put(CommonUtils.toString(prop.getKey()), CommonUtils.toString(prop.getValue()));
                }
            }
        }

    }

}
