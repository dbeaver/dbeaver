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
        Map<Object, Object> processorProperties;

        private NodeSettings(DataTransferNodeDescriptor sourceNode) throws DBException
        {
            this.sourceNode = sourceNode;
            this.settings = sourceNode.createSettings();
            this.pages = sourceNode.createWizardPages();
        }

        public IDataTransferSettings getSettings()
        {
            return settings;
        }

        public Map<Object, Object> getProcessorProperties()
        {
            return processorProperties;
        }
    }

    private List<DataTransferPipe> dataPipes;

    private DataTransferNodeDescriptor producer;
    private DataTransferNodeDescriptor consumer;

    private DataTransferProcessorDescriptor processor;
    //private Map<IStreamDataExporterDescriptor, Map<Object,Object>> exporterPropsHistory = new HashMap<IStreamDataExporterDescriptor, Map<Object, Object>>();

    private Map<Class, NodeSettings> nodeSettings = new LinkedHashMap<Class, NodeSettings>();

    private int maxJobCount = DEFAULT_THREADS_NUM;

    private transient int curPipeNum = 0;

    public DataTransferSettings(Collection<? extends IDataTransferProducer> sources)
    {
        this(sources, null);
    }

    public DataTransferSettings(Collection<? extends IDataTransferProducer> sources, Collection<? extends IDataTransferConsumer> targets)
    {
        dataPipes = new ArrayList<DataTransferPipe>();
        if (!CommonUtils.isEmpty(sources)) {
            for (IDataTransferProducer source : sources) {
                dataPipes.add(new DataTransferPipe(source, null));
            }
        } else if (!CommonUtils.isEmpty(targets)) {
            for (IDataTransferConsumer target : targets) {
                dataPipes.add(new DataTransferPipe(null, target));
            }
        } /*else if (sources.size() == targets.size()) {
            for (int i = 0; i < sources.size(); i++) {
                dataPipes.add(new DataTransferPipe(sources.get(i), targets.get(i)));
            }
        } */else {
            throw new IllegalArgumentException("Producers must match targets or must be empty");
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
            log.error("Can't add node " + node, e);
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

    public Map<Object, Object> getProcessorProperties(IDataTransferNode node)
    {
        NodeSettings nodeSettings = this.nodeSettings.get(node.getClass());
        return nodeSettings == null ? null : nodeSettings.processorProperties;
    }

    public Map<Object, Object> getProcessorProperties(IWizardPage page)
    {
        for (NodeSettings nodeSettings : this.nodeSettings.values()) {
            if (nodeSettings.pages != null) {
                for (IWizardPage nodePage : nodeSettings.pages) {
                    if (nodePage == page) {
                        return nodeSettings.processorProperties;
                    }
                }
            }
        }
        return null;
    }

    public List<DataTransferPipe> getDataPipes()
    {
        return dataPipes;
    }

    public synchronized DataTransferPipe acquireDataPipe()
    {
/*
        if (curPipeNum >= dataPipes.size()) {
            if (!folderOpened && openFolderOnFinish) {
                // Last one
                folderOpened = true;
                DBeaverUI.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        RuntimeUtils.launchProgram(outputFolder);
                    }
                });
            }
            return null;
        }
*/
        DataTransferPipe result = dataPipes.get(curPipeNum);

        curPipeNum++;
        return result;
    }

    public DataTransferNodeDescriptor getConsumer()
    {
        return consumer;
    }

    public DataTransferProcessorDescriptor getProcessor()
    {
        return processor;
    }

    void selectConsumer(DataTransferNodeDescriptor consumer, DataTransferProcessorDescriptor processor)
    {
        this.consumer = consumer;
        this.processor = processor;
        if (consumer != null && processor != null) {
            NodeSettings consumerSettings = this.nodeSettings.get(consumer.getNodeClass());
            consumerSettings.processorProperties = new HashMap<Object, Object>();
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
        // Load nodes' settings
        for (Map.Entry<Class, NodeSettings> entry : nodeSettings.entrySet()) {
            IDialogSettings nodeSection = dialogSettings.addNewSection(entry.getKey().getSimpleName());
            entry.getValue().settings.loadSettings(nodeSection);
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
    }

}
