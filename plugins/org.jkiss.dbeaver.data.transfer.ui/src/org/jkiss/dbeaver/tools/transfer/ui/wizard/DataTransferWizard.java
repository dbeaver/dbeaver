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
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
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
import org.jkiss.dbeaver.ui.dialogs.BaseWizard;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DataTransferWizard extends BaseWizard implements IExportWizard, IImportWizard {

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "DataTransfer";//$NON-NLS-1$
    private static final Log log = Log.getLog(DataTransferWizard.class);

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

    DataTransferWizard(@Nullable IDataTransferProducer[] producers, @Nullable IDataTransferConsumer[] consumers) {
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
                    }
                }
            }

            for (DataTransferNodeDescriptor node : nodes) {
                addNodeSettings(node);
            }
        }

        loadSettings();
    }

    public DBPProject getProject() {
        return NavigatorUtils.getSelectedProject();
    }

    public String getTaskId() {
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

    public boolean isProfileSelectorVisible() {
        return true;
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
        setWindowTitle(DTMessages.data_transfer_wizard_name);
        setNeedsProgressMonitor(true);
        this.currentSelection = currentSelection;
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
    public boolean canFinish() {
        for (IWizardPage page : getPages()) {
            if (isPageValid(page) && !page.isPageComplete()) {
                return false;
            }
            if (page instanceof DataTransferPageFinal && !((DataTransferPageFinal) page).isActivated()) {
                return false;
            }
        }
        return true;
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
                    for (DataTransferPipe pipe : settings.getDataPipes()) {
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
        loadFrom(
            getRunnableContext(), getDialogSettings());
    }

    private void saveSettings() {
        saveTo(getDialogSettings());
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

    public boolean isPageValid(IWizardPage page) {
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

    public IDataTransferSettings getNodeSettings(IDataTransferNode node) {
        NodePageSettings nodePageSettings = this.nodeSettings.get(node.getClass());
        return nodePageSettings == null ? null : settings.getNodeSettings(nodePageSettings.sourceNode);
    }

    void loadFrom(DBRRunnableContext runnableContext, IDialogSettings dialogSettings) {
        try {
            settings.setMaxJobCount(dialogSettings.getInt("maxJobCount"));
        } catch (NumberFormatException e) {
            settings.setMaxJobCount(DataTransferSettings.DEFAULT_THREADS_NUM);
        }
        if (dialogSettings.get("showFinalMessage") != null) {
            settings.setShowFinalMessage(dialogSettings.getBoolean("showFinalMessage"));
        }

        if (settings.isConsumerOptional() || settings.isProducerOptional()) {
            DataTransferNodeDescriptor savedConsumer = null, savedProducer = null, savedNode = null;
            {
                if (settings.isConsumerOptional()) {
                    String consumerId = dialogSettings.get("consumer");
                    if (!CommonUtils.isEmpty(consumerId)) {
                        DataTransferNodeDescriptor consumerNode = DataTransferRegistry.getInstance().getNodeById(consumerId);
                        if (consumerNode != null) {
                            settings.setConsumer(savedNode = savedConsumer = consumerNode);
                        }
                    }
                }
                if (settings.isProducerOptional()) {
                    String producerId = dialogSettings.get("producer");
                    if (!CommonUtils.isEmpty(producerId)) {
                        DataTransferNodeDescriptor producerNode = DataTransferRegistry.getInstance().getNodeById(producerId);
                        if (producerNode != null) {
                            settings.setProducer(savedNode = savedProducer = producerNode);
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
                settings.selectConsumer(savedConsumer, savedProcessor, false);
            }
            if (savedProducer != null) {
                settings.selectProducer(savedProducer, savedProcessor, false);
            }
        }

        // Load nodes' settings
        for (Map.Entry<Class, NodePageSettings> entry : nodeSettings.entrySet()) {
            IDialogSettings nodeSection = DialogSettings.getOrCreateSection(dialogSettings, entry.getKey().getSimpleName());
            IDataTransferSettings settings = this.settings.getNodeSettings(entry.getValue().sourceNode);
            if (settings != null) {
                settings.loadSettings(runnableContext, this.settings, new DialogSettingsMap(nodeSection));
            }
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
                        settings.getProcessorPropsHistory().put(nodeProcessor, props);
                        NodePageSettings nodePageSettings = this.nodeSettings.get(node.getNodeClass());
                        if (nodePageSettings != null) {

                        }
                    }
                }
            }
        }
    }

    void saveTo(IDialogSettings dialogSettings) {
        dialogSettings.put("maxJobCount", settings.getMaxJobCount());
        dialogSettings.put("showFinalMessage", settings.isShowFinalMessage());
        // Save nodes' settings
        for (Map.Entry<Class, NodePageSettings> entry : nodeSettings.entrySet()) {
            IDialogSettings nodeSection = DialogSettings.getOrCreateSection(dialogSettings, entry.getKey().getSimpleName());
            IDataTransferSettings settings = this.settings.getNodeSettings(entry.getValue().sourceNode);
            if (settings != null) {
                settings.saveSettings(new DialogSettingsMap(nodeSection));
            }
        }

        if (settings.getProducer() != null) {
            dialogSettings.put("producer", settings.getProducer().getId());
        }
        if (settings.getConsumer() != null) {
            dialogSettings.put("consumer", settings.getConsumer().getId());
        }
        if (settings.getProcessor() != null) {
            dialogSettings.put("processor", settings.getProcessor().getId());
        }

        // Save processors' properties
        IDialogSettings processorsSection = dialogSettings.addNewSection("processors");
        for (DataTransferProcessorDescriptor procDescriptor : settings.getProcessorPropsHistory().keySet()) {
            IDialogSettings procSettings = processorsSection.addNewSection(procDescriptor.getFullId());
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
        }

    }

}