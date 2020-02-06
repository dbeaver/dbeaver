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
package org.jkiss.dbeaver.ui.controls.resultset.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.ContributionManager;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Open results in external application
 */
public class ResultSetHandlerOpenWith extends AbstractHandler implements IElementUpdater {

    private static final Log log = Log.getLog(ResultSetHandlerOpenWith.class);

    public static final String CMD_OPEN_WITH = "org.jkiss.dbeaver.core.resultset.openWith";
    public static final String PARAM_PROCESSOR_ID = "processorId";

    public static final String PARAM_ACTIVE_APP = "org.jkiss.dbeaver.core.resultset.openWith.currentApp";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        DataTransferProcessorDescriptor processor = getActiveProcessor(event.getParameter(PARAM_PROCESSOR_ID));

        if (processor == null) {
            return null;
        }
        switch (event.getCommand().getId()) {
            case CMD_OPEN_WITH:
                openResultsWith(resultSet, processor);
                break;
        }
        return null;
    }

    static DataTransferProcessorDescriptor getActiveProcessor(String processorId) {
        if (CommonUtils.isEmpty(processorId)) {
            processorId = DBWorkbench.getPlatform().getPreferenceStore().getString(PARAM_ACTIVE_APP);
        }
        if (CommonUtils.isEmpty(processorId)) {
            DataTransferProcessorDescriptor defaultAppProcessor = getDefaultProcessor();
            if (defaultAppProcessor != null) {
                return defaultAppProcessor;
            }
        } else {
            return DataTransferRegistry.getInstance().getProcessor(processorId);
        }
        return null;
    }

    static DataTransferProcessorDescriptor getDefaultProcessor() {
        DataTransferProcessorDescriptor defaultAppProcessor = getDefaultAppProcessor();
        if (defaultAppProcessor != null) {
            return defaultAppProcessor;
        }
        return null;
    }

    private static void openResultsWith(IResultSetController resultSet, DataTransferProcessorDescriptor processor) {

        ResultSetDataContainerOptions options = new ResultSetDataContainerOptions();

        IResultSetSelection rsSelection = resultSet.getSelection();
        List<ResultSetRow> rsSelectedRows = rsSelection.getSelectedRows();
        List<DBDAttributeBinding> rsSelectedAttributes = rsSelection.getSelectedAttributes();
        if (rsSelectedRows.size() > 1 || rsSelectedAttributes.size() > 1) {
            List<Long> selectedRows = new ArrayList<>();
            for (ResultSetRow selectedRow : rsSelectedRows) {
                selectedRows.add((long) selectedRow.getRowNumber());
            }
            List<String> selectedAttributes = new ArrayList<>();
            for (DBDAttributeBinding attributeBinding : rsSelectedAttributes) {
                selectedAttributes.add(attributeBinding.getName());
            }

            options.setSelectedRows(selectedRows);
            options.setSelectedColumns(selectedAttributes);
        }
        ResultSetDataContainer dataContainer = new ResultSetDataContainer(resultSet, options);
        if (dataContainer.getDataSource() == null) {
            DBWorkbench.getPlatformUI().showError("Open " + processor.getAppName(), ModelMessages.error_not_connected_to_database);
            return;
        }

        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        String prevActiveApp = preferenceStore.getString(PARAM_ACTIVE_APP);
        if (!CommonUtils.equalObjects(prevActiveApp, processor.getFullId())) {
            //preferenceStore.setValue(PARAM_ACTIVE_APP, processor.getFullId());
            //resultSet.updateEditControls();
            //resultSet.getControl().layout(true);
        }

        AbstractJob exportJob = new AbstractJob("Open " + processor.getAppName()) {

            {
                setUser(true);
                setSystem(false);
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    File tempDir = DBWorkbench.getPlatform().getTempFolder(monitor, "data-files");
                    File tempFile = new File(tempDir, new SimpleDateFormat(
                        "yyyyMMdd-HHmmss").format(System.currentTimeMillis()) + "." + processor.getAppFileExtension());
                    tempFile.deleteOnExit();

                    IDataTransferProcessor processorInstance = processor.getInstance();
                    if (!(processorInstance instanceof IStreamDataExporter)) {
                        return Status.CANCEL_STATUS;
                    }
                    IStreamDataExporter exporter = (IStreamDataExporter) processorInstance;

                    StreamTransferConsumer consumer = new StreamTransferConsumer();
                    StreamConsumerSettings settings = new StreamConsumerSettings();

                    settings.setOutputEncodingBOM(false);
                    settings.setOpenFolderOnFinish(false);
                    settings.setOutputFolder(tempDir.getAbsolutePath());
                    settings.setOutputFilePattern(tempFile.getName());

                    Map<Object, Object> properties = new HashMap<>();
                    for (DBPPropertyDescriptor prop : processor.getProperties()) {
                        properties.put(prop.getId(), prop.getDefaultValue());
                    }
                    // Remove extension property (we specify file name directly)
                    properties.remove(StreamConsumerSettings.PROP_FILE_EXTENSION);

                    consumer.initTransfer(
                        dataContainer,
                        settings,
                        new IDataTransferConsumer.TransferParameters(processor.isBinaryFormat(), processor.isHTMLFormat()),
                        exporter,
                        properties);

                    DBDDataFilter dataFilter = resultSet.getModel().getDataFilter();
                    DatabaseTransferProducer producer = new DatabaseTransferProducer(dataContainer, dataFilter);
                    DatabaseProducerSettings producerSettings = new DatabaseProducerSettings();
                    producerSettings.setExtractType(DatabaseProducerSettings.ExtractType.SINGLE_QUERY);
                    producerSettings.setQueryRowCount(false);
                    // disable OpenNewconnection by default (#6432)
                    producerSettings.setOpenNewConnections(false);
                    producerSettings.setSelectedRowsOnly(!CommonUtils.isEmpty(options.getSelectedRows()));
                    producerSettings.setSelectedColumnsOnly(!CommonUtils.isEmpty(options.getSelectedColumns()));

                    producer.transferData(monitor, consumer, null, producerSettings, null);

                    consumer.finishTransfer(monitor, false);

                    UIUtils.asyncExec(() -> {
                        if (!UIUtils.launchProgram(tempFile.getAbsolutePath())) {
                            DBWorkbench.getPlatformUI().showError(
                                "Open " + processor.getAppName(),
                                "Can't open " + processor.getAppFileExtension() + " file '" + tempFile.getAbsolutePath() + "'");
                        }
                    });
                } catch (Exception e) {
                    DBWorkbench.getPlatformUI().showError("Error opening in " + processor.getAppName(), null, e);
                }
                return Status.OK_STATUS;
            }
        };
        exportJob.schedule();
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        // Put processor name in command label
        DataTransferProcessorDescriptor processor = getActiveProcessor((String) parameters.get(PARAM_PROCESSOR_ID));
        if (processor != null) {
            element.setText(processor.getAppName());
            if (!CommonUtils.isEmpty(processor.getDescription())) {
                element.setTooltip(processor.getDescription());
            }
            if (processor.getIcon() != null) {
                element.setIcon(DBeaverIcons.getImageDescriptor(processor.getIcon()));
            }
        }
    }

    private static DataTransferProcessorDescriptor getDefaultAppProcessor() {
        List<DataTransferProcessorDescriptor> processors = new ArrayList<>();
        for (final DataTransferNodeDescriptor consumerNode : DataTransferRegistry.getInstance().getNodes(DataTransferNodeDescriptor.NodeType.CONSUMER)) {
            for (DataTransferProcessorDescriptor processor : consumerNode.getProcessors()) {
                if (processor.getAppFileExtension() != null) {
                    processors.add(processor);
                }
            }
        }
        processors.sort(Comparator.comparingInt(DataTransferProcessorDescriptor::getOrder));
        return processors.isEmpty() ? null : processors.get(0);
    }

    public static class OpenWithParameterValues implements IParameterValues {

        @Override
        public Map<String,String> getParameterValues() {
            final Map<String,String> values = new HashMap<>();

            for (final DataTransferNodeDescriptor consumerNode : DataTransferRegistry.getInstance().getNodes(DataTransferNodeDescriptor.NodeType.CONSUMER)) {
                for (DataTransferProcessorDescriptor processor : consumerNode.getProcessors()) {
                    if (processor.getAppFileExtension() != null) {
                        values.put(processor.getAppName(), processor.getFullId());
                    }
                }
            }

            return values;
        }

    }

    public static class OpenWithMenuContributor extends CompoundContributionItem
    {
        @Override
        protected IContributionItem[] getContributionItems() {
            final ResultSetViewer rsv = (ResultSetViewer) ResultSetHandlerMain.getActiveResultSet(
                UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart());
            if (rsv == null) {
                return new IContributionItem[0];
            }
            ContributionManager menu = new MenuManager();
            fillOpenWithMenu(rsv, menu);
            return menu.getItems();
        }
    }

    public static void fillOpenWithMenu(ResultSetViewer viewer, IContributionManager openWithMenu) {

        ResultSetDataContainerOptions options = new ResultSetDataContainerOptions();
        ResultSetDataContainer dataContainer = new ResultSetDataContainer(viewer, options);

        List<DataTransferProcessorDescriptor> appProcessors = new ArrayList<>();

        for (final DataTransferNodeDescriptor consumerNode : DataTransferRegistry.getInstance().getAvailableConsumers(Collections.singleton(dataContainer))) {
            for (DataTransferProcessorDescriptor processor : consumerNode.getProcessors()) {
                if (processor.getAppFileExtension() != null) {
                    appProcessors.add(processor);
                }
            }
        }

        appProcessors.sort(Comparator.comparingInt(DataTransferProcessorDescriptor::getOrder));

        for (DataTransferProcessorDescriptor processor : appProcessors) {
            CommandContributionItemParameter params = new CommandContributionItemParameter(
                viewer.getSite(),
                processor.getId(),
                ResultSetHandlerOpenWith.CMD_OPEN_WITH,
                CommandContributionItem.STYLE_RADIO
            );
            params.label = processor.getAppName();
            if (processor.getIcon() != null) {
                params.icon = DBeaverIcons.getImageDescriptor(processor.getIcon());
            }
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(ResultSetHandlerOpenWith.PARAM_PROCESSOR_ID, processor.getFullId());
            params.parameters = parameters;
            openWithMenu.add(new CommandContributionItem(params));
        }
    }


}
