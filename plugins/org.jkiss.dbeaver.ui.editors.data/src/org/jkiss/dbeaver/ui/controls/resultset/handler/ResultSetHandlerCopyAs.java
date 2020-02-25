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
import org.eclipse.jface.action.*;
import org.eclipse.ui.IWorkbenchPartSite;
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
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
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
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Copy results in external format
 */
public class ResultSetHandlerCopyAs extends AbstractHandler implements IElementUpdater {

    private static final Log log = Log.getLog(ResultSetHandlerCopyAs.class);

    public static final String CMD_COPY_AS = "org.jkiss.dbeaver.core.resultset.copyAs";
    public static final String PARAM_PROCESSOR_ID = "processorId";

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
            case CMD_COPY_AS:
                openResultsWith(resultSet, processor);
                break;
        }
        return null;
    }

    static DataTransferProcessorDescriptor getActiveProcessor(String processorId) {
        if (CommonUtils.isEmpty(processorId)) {
            return null;
        } else {
            return DataTransferRegistry.getInstance().getProcessor(processorId);
        }
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
            DBWorkbench.getPlatformUI().showError("Copy As " + processor.getName(), ModelMessages.error_not_connected_to_database);
            return;
        }

        AbstractJob exportJob = new AbstractJob("Copy As " + processor.getName()) {

            {
                setUser(true);
                setSystem(false);
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    IDataTransferProcessor processorInstance = processor.getInstance();
                    if (!(processorInstance instanceof IStreamDataExporter)) {
                        return Status.CANCEL_STATUS;
                    }
                    IStreamDataExporter exporter = (IStreamDataExporter) processorInstance;

                    StreamTransferConsumer consumer = new StreamTransferConsumer();
                    StreamConsumerSettings settings = new StreamConsumerSettings();

                    settings.setOutputClipboard(true);
                    settings.setOutputEncodingBOM(false);
                    settings.setOpenFolderOnFinish(false);

                    Map<Object, Object> properties = new HashMap<>();
                    for (DBPPropertyDescriptor prop : processor.getProperties()) {
                        properties.put(prop.getId(), prop.getDefaultValue());
                    }

                    consumer.initTransfer(
                        dataContainer,
                        settings,
                        new IDataTransferConsumer.TransferParameters(processor.isBinaryFormat(), processor.isHTMLFormat()),
                        exporter,
                        properties);

                    DBDDataFilter dataFilter = resultSet.getModel().getDataFilter();
                    DatabaseTransferProducer producer = new DatabaseTransferProducer(dataContainer, dataFilter);
                    DatabaseProducerSettings producerSettings = new DatabaseProducerSettings();
                    producerSettings.setOpenNewConnections(false);
                    if (resultSet.isHasMoreData()) {
                        // For long resultsets we may need to open new connection
                        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                        if (serviceSQL != null) {
                            producerSettings.setOpenNewConnections(serviceSQL.useIsolatedConnections(resultSet));
                        }
                    }
                    producerSettings.setExtractType(DatabaseProducerSettings.ExtractType.SINGLE_QUERY);
                    producerSettings.setQueryRowCount(false);
                    producerSettings.setSelectedRowsOnly(!CommonUtils.isEmpty(options.getSelectedRows()));
                    producerSettings.setSelectedColumnsOnly(!CommonUtils.isEmpty(options.getSelectedColumns()));

                    producer.transferData(monitor, consumer, null, producerSettings, null);

                    consumer.finishTransfer(monitor, false);
                    consumer.finishTransfer(monitor, true);
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
            String commandName = ActionUtils.findCommandName(CMD_COPY_AS);
            element.setText(commandName + " " + processor.getName());
            if (!CommonUtils.isEmpty(processor.getDescription())) {
                element.setTooltip(processor.getDescription());
            }
            if (processor.getIcon() != null) {
                element.setIcon(DBeaverIcons.getImageDescriptor(processor.getIcon()));
            }
        }
    }

    public static class CopyAsParameterValues implements IParameterValues {

        @Override
        public Map<String,String> getParameterValues() {
            final Map<String,String> values = new HashMap<>();

            for (final DataTransferNodeDescriptor consumerNode : DataTransferRegistry.getInstance().getNodes(DataTransferNodeDescriptor.NodeType.CONSUMER)) {
                for (DataTransferProcessorDescriptor processor : consumerNode.getProcessors()) {
                    if (processor.isBinaryFormat()) {
                        continue;
                    }
                    values.put(processor.getName(), processor.getFullId());
                }
            }

            return values;
        }

    }

    public static class CopyAsMenuContributor extends CompoundContributionItem
    {
        @Override
        protected IContributionItem[] getContributionItems() {
            final ResultSetViewer rsv = (ResultSetViewer) ResultSetHandlerMain.getActiveResultSet(
                UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart());
            if (rsv == null) {
                return new IContributionItem[0];
            }
            ContributionManager menu = new MenuManager();
            fillCopyAsMenu(rsv, menu);
            return menu.getItems();
        }
    }

    public static void fillCopyAsMenu(ResultSetViewer viewer, IContributionManager copyAsMenu) {

        IWorkbenchPartSite site = viewer.getSite();
        copyAsMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerCopySpecial.CMD_COPY_SPECIAL));
        copyAsMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerCopySpecial.CMD_COPY_COLUMN_NAMES));
        copyAsMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_COPY_ROW_NAMES));
        // Add copy commands for different formats
        copyAsMenu.add(new Separator());

        ResultSetDataContainerOptions options = new ResultSetDataContainerOptions();
        ResultSetDataContainer dataContainer = new ResultSetDataContainer(viewer, options);

        List<DataTransferProcessorDescriptor> appProcessors = new ArrayList<>();

        for (final DataTransferNodeDescriptor consumerNode : DataTransferRegistry.getInstance().getAvailableConsumers(Collections.singleton(dataContainer))) {
            for (DataTransferProcessorDescriptor processor : consumerNode.getProcessors()) {
                if (processor.isBinaryFormat()) {
                    continue;
                }
                appProcessors.add(processor);
            }
        }

        appProcessors.sort(Comparator.comparing(DataTransferProcessorDescriptor::getName));

        for (DataTransferProcessorDescriptor processor : appProcessors) {
            CommandContributionItemParameter params = new CommandContributionItemParameter(
                site,
                processor.getId(),
                ResultSetHandlerCopyAs.CMD_COPY_AS,
                CommandContributionItem.STYLE_PUSH
            );
            params.label = processor.getName();
            if (processor.getIcon() != null) {
                params.icon = DBeaverIcons.getImageDescriptor(processor.getIcon());
            }
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(ResultSetHandlerCopyAs.PARAM_PROCESSOR_ID, processor.getFullId());
            params.parameters = parameters;
            copyAsMenu.add(new CommandContributionItem(params));
        }
    }

}
