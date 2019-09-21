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
package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.tools.transfer.*;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIActivator;
import org.jkiss.dbeaver.tools.transfer.ui.registry.DataTransferConfiguratorRegistry;
import org.jkiss.dbeaver.tools.transfer.ui.registry.DataTransferNodeConfiguratorDescriptor;
import org.jkiss.dbeaver.tools.transfer.ui.registry.DataTransferPageDescriptor;
import org.jkiss.dbeaver.tools.transfer.ui.registry.DataTransferPageType;
import org.jkiss.dbeaver.ui.DialogSettingsMap;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.task.TaskConfigurationWizard;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DataTransferWizard extends TaskConfigurationWizard implements IExportWizard, IImportWizard {

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "DataTransfer";//$NON-NLS-1$
    //private static final Log log = Log.getLog(DataTransferWizard.class);

    public static class NodePageSettings {
        DataTransferNodeDescriptor sourceNode;
        DataTransferNodeConfiguratorDescriptor nodeConfigurator;
        IWizardPage[] pages;
        IWizardPage settingsPage;

        private NodePageSettings(DataTransferNodeDescriptor sourceNode, DataTransferNodeConfiguratorDescriptor nodeConfigurator, boolean consumerOptional, boolean producerOptional) {
            this.sourceNode = sourceNode;
            this.nodeConfigurator = nodeConfigurator;
            this.pages = nodeConfigurator == null ? new IWizardPage[0] : nodeConfigurator.createWizardPages(consumerOptional, producerOptional, false);
            IWizardPage[] sPages = nodeConfigurator == null ? new IWizardPage[0] : nodeConfigurator.createWizardPages(consumerOptional, producerOptional, true);
            // There can be only one settings page per node
            this.settingsPage = sPages.length == 0 ? null : sPages[0];
        }

    }

    private DataTransferSettings settings;
    private IStructuredSelection currentSelection;
    private Map<Class, NodePageSettings> nodeSettings = new LinkedHashMap<>();

    DataTransferWizard(@NotNull DBRRunnableContext runnableContext, DBTTask task) {
        this(
            runnableContext,
            getNodesFromLocation(runnableContext, task, "producers", IDataTransferProducer.class),
            getNodesFromLocation(runnableContext, task, "consumers", IDataTransferConsumer.class),
            task);
    }

    DataTransferWizard(@NotNull DBRRunnableContext runnableContext, @Nullable Collection<IDataTransferProducer> producers, @Nullable Collection<IDataTransferConsumer> consumers, @Nullable DBTTask task) {
        super(task);
        this.settings = new DataTransferSettings(producers, consumers);

        setDialogSettings(
            UIUtils.getSettingsSection(
                DTUIActivator.getDefault().getDialogSettings(),
                RS_EXPORT_WIZARD_DIALOG_SETTINGS));

        {
            // Load node settings
            Collection<DBSObject> objectTypes = settings.getSourceObjects();
            List<DataTransferNodeDescriptor> nodes = new ArrayList<>();
            DataTransferRegistry registry = DataTransferRegistry.getInstance();
            if (CommonUtils.isEmpty(producers)) {
                nodes.addAll(registry.getAvailableProducers(objectTypes));
            } else {
                for (IDataTransferProducer source : producers) {
                    DataTransferNodeDescriptor node = registry.getNodeByType(source.getClass());
                    if (node != null && !nodes.contains(node)) {
                        nodes.add(node);
                    }
                }
            }
            if (CommonUtils.isEmpty(consumers)) {
                nodes.addAll(registry.getAvailableConsumers(objectTypes));
            } else {
                for (IDataTransferConsumer target : consumers) {
                    DataTransferNodeDescriptor node = registry.getNodeByType(target.getClass());
                    if (node != null && !nodes.contains(node)) {
                        nodes.add(node);
                    }
                }
            }

            for (DataTransferNodeDescriptor node : nodes) {
                addNodeSettings(node);
            }
        }

        Map<String, Object> configuration = task == null ? null : JSONUtils.getObject(task.getProperties(), "configuration");
        if (configuration != null) {
            loadConfiguration(runnableContext, configuration);
        } else {
            loadSettings();
        }
    }

    @Override
    public String getTaskTypeId() {
        if (getSettings().isProducerOptional()) {
            return DTConstants.TASK_IMPORT;
        } else {
            return DTConstants.TASK_EXPORT;
        }
    }

    public IStructuredSelection getCurrentSelection() {
        return currentSelection;
    }

    public DataTransferSettings getSettings() {
        return settings;
    }

    public <T extends IDataTransferSettings> T getPageSettings(IWizardPage page, Class<T> type) {
        return type.cast(getNodeSettings(page));
    }

    @Override
    public void addPages() {
        super.addPages();
        if (settings.isConsumerOptional() || settings.isProducerOptional()) {
            addPage(new DataTransferPagePipes());
        }
        addWizardPages(this);
        addPage(new DataTransferPageFinal());
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        updateWizardTitle();
        setNeedsProgressMonitor(true);
        this.currentSelection = currentSelection;
    }

    @Override
    protected String getDefaultWindowTitle() {
        return DTMessages.data_transfer_wizard_name;
    }

    @Nullable
    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        IWizardPage[] pages = getPages();
        int curIndex = -1;
        for (int i = 0; i < pages.length; i++) {
            if (pages[i] == page) {
                curIndex = i;
                break;
            }
        }
        if (curIndex == pages.length - 1) {
            return null;
        }
        if (curIndex != -1) {
            // Return first node config page
            for (int i = curIndex + 1; i < pages.length; i++) {
                if (isPageValid(pages[i])) {
                    return pages[i];
                }
            }
        }
        // Final page
        return pages[pages.length - 1];
    }

    @Nullable
    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        IWizardPage[] pages = getPages();
        int curIndex = -1;
        for (int i = 0; i < pages.length; i++) {
            if (pages[i] == page) {
                curIndex = i;
                break;
            }
        }
        if (curIndex == 0) {
            return null;
        }
        if (curIndex != -1) {
            for (int i = curIndex - 1; i > 0; i--) {
                if (isPageValid(pages[i])) {
                    return pages[i];
                }
            }
        }
        // First page
        return pages[0];
    }

    @Override
    public boolean performCancel() {
        // Save settings anyway?
        //saveSettings();

        return super.performCancel();
    }

    @Override
    public boolean performFinish() {
        // Save settings
        saveSettings();
        DTUIActivator.getDefault().saveDialogSettings();

        // Start consumers
        try {
            UIUtils.run(getContainer(), true, true, monitor -> {
                try {
                    List<DataTransferPipe> dataPipes = settings.getDataPipes();
                    for (int i = 0; i < dataPipes.size(); i++) {
                        DataTransferPipe pipe = dataPipes.get(i);
                        pipe.initPipe(getSettings(), i, dataPipes.size());
                        pipe.getConsumer().startTransfer(monitor);
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Transfer init failed", "Can't start data transfer", e.getTargetException());
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        // Run export jobs
        executeJobs();

        // Done
        return true;
    }

    @Override
    public void setContainer(IWizardContainer wizardContainer) {
        super.setContainer(wizardContainer);
        //wizardContainer.
    }

    private void loadSettings() {
        loadConfiguration(
            getRunnableContext(), new DialogSettingsMap(getDialogSettings()));
    }

    private void saveSettings() {
        DialogSettingsMap dialogSettings = new DialogSettingsMap(getDialogSettings());
        saveConfiguration(dialogSettings);
    }

    private void executeJobs() {
        // Schedule jobs for data providers
        int totalJobs = settings.getDataPipes().size();
        if (totalJobs > settings.getMaxJobCount()) {
            totalJobs = settings.getMaxJobCount();
        }
        for (int i = 0; i < totalJobs; i++) {
            DataTransferJob job = new DataTransferJob(settings);
            job.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    // Run async to avoid blocking progress monitor dialog
                    UIUtils.asyncExec(() -> {
                        // Make a sound
                        Display.getCurrent().beep();
                        // Notify agent
                        long time = job.getElapsedTime();
                        boolean hasErrors = job.isHasErrors();
                        DBPPlatformUI platformUI = DBWorkbench.getPlatformUI();
                        if (time > platformUI.getLongOperationTimeout() * 1000) {
                            platformUI.notifyAgent(
                                "Data transfer completed", !hasErrors ? IStatus.INFO : IStatus.ERROR);
                        }
                        if (settings.isShowFinalMessage() && !hasErrors) {
                            // Show message box
                            UIUtils.showMessageBox(
                                null,
                                DTMessages.data_transfer_wizard_name,
                                "Data transfer completed (" + RuntimeUtils.formatExecutionTime(time) + ")",
                                SWT.ICON_INFORMATION);
                        }
                    });
                }
            });
            job.schedule();
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

        DataTransferNodeConfiguratorDescriptor configurator = DataTransferConfiguratorRegistry.getInstance().getConfigurator(node.getId());
        nodeSettings.put(nodeClass, new NodePageSettings(node, configurator, settings.isConsumerOptional(), settings.isProducerOptional()));
    }

    void addWizardPages(DataTransferWizard wizard) {
        List<IWizardPage> settingPages = new ArrayList<>();
        // Add regular pages
        for (NodePageSettings nodePageSettings : this.nodeSettings.values()) {
            for (IWizardPage page : nodePageSettings.pages) {
                if (nodePageSettings.nodeConfigurator == null || nodePageSettings.nodeConfigurator.getPageDescriptor(page).getPageType() == DataTransferPageType.PREVIEW) {
                    // Add later
                    continue;
                }
                wizard.addPage(page);
            }
            if (nodePageSettings.settingsPage != null) {
                settingPages.add(nodePageSettings.settingsPage);
            }
        }
        // Add common settings page
        if (!CommonUtils.isEmpty(settingPages)) {
            wizard.addPage(new DataTransferPageSettings());
        }
        // Add preview pages
        for (NodePageSettings nodePageSettings : this.nodeSettings.values()) {
            for (IWizardPage page : nodePageSettings.pages) {
                if (nodePageSettings.nodeConfigurator != null && nodePageSettings.nodeConfigurator.getPageDescriptor(page).getPageType() == DataTransferPageType.PREVIEW) {
                    wizard.addPage(page);
                }
            }
        }
    }

    protected boolean isPageValid(IWizardPage page) {
        return page instanceof DataTransferPageSettings || isPageValid(page, settings.getProducer()) || isPageValid(page, settings.getConsumer());
    }

    private boolean isPageValid(IWizardPage page, DataTransferNodeDescriptor node) {
        NodePageSettings nodePageSettings = node == null ? null : this.nodeSettings.get(node.getNodeClass());
        if (nodePageSettings != null && ArrayUtils.contains(nodePageSettings.pages, page)) {
            // Check does page matches consumer and producer
            for (NodePageSettings ns : this.nodeSettings.values()) {
                DataTransferPageDescriptor pd = ns.nodeConfigurator == null ? null : ns.nodeConfigurator.getPageDescriptor(page);
                if (pd != null) {
                    if (pd.getProducerType() != null && settings.getProducer() != null && !settings.getProducer().getId().equals(pd.getProducerType())) {
                        // Producer doesn't match
                        return false;
                    }
                    if (pd.getConsumerType() != null && settings.getConsumer() != null && !settings.getConsumer().getId().equals(pd.getConsumerType())) {
                        // Consumer doesn't match
                        return false;
                    }
                }
            }

            return true;
        }
        return false;
    }

    NodePageSettings getNodeInfo(IDataTransferNode node) {
        return this.nodeSettings.get(node.getClass());
    }

    public IDataTransferSettings getNodeSettings(IWizardPage page) {
        for (NodePageSettings nodePageSettings : this.nodeSettings.values()) {
            if (page == nodePageSettings.settingsPage) {
                return settings.getNodeSettings(nodePageSettings.sourceNode);
            }
            if (nodePageSettings.pages != null) {
                for (IWizardPage nodePage : nodePageSettings.pages) {
                    if (nodePage == page) {
                        return settings.getNodeSettings(nodePageSettings.sourceNode);
                    }
                }
            }
        }
        return null;
    }

    private void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        settings.setMaxJobCount(CommonUtils.toInt(config.get("maxJobCount"), DataTransferSettings.DEFAULT_THREADS_NUM));
        settings.setShowFinalMessage(CommonUtils.getBoolean(config.get("showFinalMessage"), settings.isShowFinalMessage()));

        DataTransferNodeDescriptor savedConsumer = null, savedProducer = null, processorNode = null;
        {
            {
                String consumerId = CommonUtils.toString(config.get("consumer"));
                if (!CommonUtils.isEmpty(consumerId)) {
                    DataTransferNodeDescriptor consumerNode = DataTransferRegistry.getInstance().getNodeById(consumerId);
                    if (consumerNode != null) {
                        settings.setConsumer(savedConsumer = consumerNode);
                        if (settings.isConsumerOptional()) {
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
                        settings.setProducer(savedProducer = producerNode);
                        if (settings.isProducerOptional()) {
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
        if (settings.isConsumerOptional() && savedConsumer != null) {
            settings.selectConsumer(savedConsumer, savedProcessor, false);
        }
        if (settings.isProducerOptional() && savedProducer != null) {
            settings.selectProducer(savedProducer, savedProcessor, false);
        }


        // Load nodes' settings
        for (Map.Entry<Class, NodePageSettings> entry : nodeSettings.entrySet()) {
            Map<String, Object> nodeSection = JSONUtils.getObject(config, entry.getKey().getSimpleName());
            IDataTransferSettings settings = this.settings.getNodeSettings(entry.getValue().sourceNode);
            if (settings != null) {
                settings.loadSettings(runnableContext, this.settings, nodeSection);
            }
        }
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
                        settings.getProcessorPropsHistory().put(nodeProcessor, props);
                        NodePageSettings nodePageSettings = this.nodeSettings.get(node.getNodeClass());
                        if (nodePageSettings != null) {

                        }
                    }
                }
            }
        }
    }

    private static <T> List<T> getNodesFromLocation(@NotNull DBRRunnableContext runnableContext, DBTTask task, String nodeType, Class<T> nodeClass) {
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

    public void saveTaskState(Map<String, Object> state) {
        List<IDataTransferNode> producers = new ArrayList<>();
        List<IDataTransferNode> consumers = new ArrayList<>();
        for (DataTransferPipe pipe : settings.getDataPipes()) {
            if (pipe.getProducer() != null) {
                producers.add(pipe.getProducer());
            }
            if (pipe.getConsumer() != null) {
                consumers.add(pipe.getConsumer());
            }
        }
        saveNodesLocation(state, producers, "producers");
        saveNodesLocation(state, consumers, "consumers");
        state.put("configuration", saveConfiguration(new LinkedHashMap<>()));
    }

    private static void saveNodesLocation(Map<String, Object> state, Collection<IDataTransferNode> nodes, String nodeType) {
        if (nodes != null) {
            List<Map<String, Object>> inputObjects = new ArrayList<>();
            for (Object inputObject : nodes) {
                inputObjects.add(JSONUtils.serializeObject(inputObject));
            }
            state.put(nodeType, inputObjects);
        }
    }

    Map<String, Object> saveConfiguration(Map<String, Object> config) {
        config.put("maxJobCount", settings.getMaxJobCount());
        config.put("showFinalMessage", settings.isShowFinalMessage());
        // Save nodes' settings
        for (Map.Entry<Class, NodePageSettings> entry : nodeSettings.entrySet()) {
            //IDialogSettings nodeSection = DialogSettings.getOrCreateSection(dialogSettings, entry.getKey().getSimpleName());
            Map<String, Object> nodeSection = new LinkedHashMap<>();

            IDataTransferSettings settings = this.settings.getNodeSettings(entry.getValue().sourceNode);
            if (settings != null) {
                settings.saveSettings(nodeSection);
            }
            // Note: do it in the end because of limitation of IDialogSettings wrapper
            config.put(entry.getKey().getSimpleName(), nodeSection);
        }

        if (settings.getProducer() != null) {
            config.put("producer", settings.getProducer().getId());
        }
        if (settings.getConsumer() != null) {
            config.put("consumer", settings.getConsumer().getId());
        }
        if (settings.getProcessor() != null) {
            config.put("processor", settings.getProcessor().getId());
        }

        // Save processors' properties
        Map<String, Object> processorsSection = new LinkedHashMap<>();

        config.put("processors", processorsSection);
        for (DataTransferProcessorDescriptor procDescriptor : settings.getProcessorPropsHistory().keySet()) {

            Map<String, Object> procSettings = new LinkedHashMap<>();

            Map<Object, Object> props = settings.getProcessorPropsHistory().get(procDescriptor);
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
            processorsSection.put(procDescriptor.getFullId(), procSettings);
        }
        return config;
    }

}