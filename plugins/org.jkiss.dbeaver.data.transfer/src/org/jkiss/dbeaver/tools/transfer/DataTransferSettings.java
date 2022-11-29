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
package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.runtime.MonitorRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskSettings;
import org.jkiss.dbeaver.model.task.DBTaskUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DataTransferSettings
 */
public class DataTransferSettings implements DBTTaskSettings<DBPObject> {
    private static final Log log = Log.getLog(DataTransferSettings.class);

    public static final int DEFAULT_THREADS_NUM = 1;

    private final DataTransferState state;
    private final Map<String, Object> configurationMap;
    private List<DataTransferPipe> dataPipes;

    private DataTransferNodeDescriptor producer;
    private DataTransferNodeDescriptor consumer;

    private final Map<DataTransferNodeDescriptor, IDataTransferSettings> nodeSettings = new HashMap<>();

    private DataTransferProcessorDescriptor processor;
    private final Map<DataTransferProcessorDescriptor, Map<String, Object>> processorPropsHistory = new HashMap<>();
    private boolean producerProcessor;

    private IDataTransferProducer<?>[] initProducers;
    private @Nullable IDataTransferConsumer<?,?>[] initConsumers;
    private final List<DBSObject> initObjects = new ArrayList<>();

    private boolean consumerOptional;
    private boolean producerOptional;
    private int maxJobCount = DEFAULT_THREADS_NUM;

    private transient boolean nodeSettingsLoaded = false;

    private transient int curPipeNum = 0;

    private boolean showFinalMessage = true;
    // Hacky flag. Says that pipe selection is frozen.
    // Makes sense for special case like multi-file import
    private boolean pipeChangeRestricted;
    // Hacky flag too. Skip nodes (producer and consumer) update
    // if it's not required -- e.g., when we're editing an exiting task
    private final boolean nodeUpdateRestricted;
    // New hacky flag. Helps to understand - do the task running already? Or are we just checking task settings?
    private boolean isTaskRunning;

    public DataTransferSettings(
        @Nullable Collection<? extends IDataTransferProducer> producers,
        @Nullable Collection<? extends IDataTransferConsumer> consumers,
        @NotNull Map<String, Object> configuration,
        @NotNull DataTransferState state,
        boolean selectDefaultNodes,
        boolean isExport,
        boolean isExitingTask,
        boolean isTaskRunning)
    {
        this.state = state;
        this.nodeUpdateRestricted = isExitingTask;
        this.configurationMap = configuration;
        this.isTaskRunning = isTaskRunning;
        initializePipes(producers, consumers, isExport);
        loadSettings(configuration);

        if (!selectDefaultNodes) {
            // Now cleanup all nodes. We needed them only to load default producer/consumer settings
            this.producer = null;
            this.consumer = null;
            this.processor = null;
        }
    }

    public DataTransferSettings(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBTTask task,
        @NotNull Log taskLog,
        @NotNull Map<String, Object> configuration,
        @NotNull DataTransferState state,
        boolean isTaskRunning) {
        this(
            getNodesFromLocation(monitor, task, state, taskLog, "producers", IDataTransferProducer.class),
            getNodesFromLocation(monitor, task, state, taskLog, "consumers", IDataTransferConsumer.class),
            getTaskOrSavedSettings(task, configuration),
            state,
            !task.getProperties().isEmpty(),
            isExportTask(task),
            DBTaskUtils.isTaskExists(task),
            isTaskRunning
        );
    }

    public DataTransferState getState() {
        return state;
    }

    public static boolean isExportTask(DBTTask task) {
        return task.getType().getId().equals(DTConstants.TASK_EXPORT);
    }

    // When we create new task its settings are empty. We need to load defaults from saved settings (usually data transfer dialog settings)
    private static Map<String, Object> getTaskOrSavedSettings(@NotNull DBTTask task, @NotNull Map<String, Object> savedSettings) {
        Map<String, Object> taskSettings = JSONUtils.getObject(task.getProperties(), "configuration");
        if (taskSettings.isEmpty() && !savedSettings.isEmpty()) {
            return savedSettings;
        }
        return taskSettings;
    }

    public boolean isPipeChangeRestricted() {
        return pipeChangeRestricted;
    }

    public void setPipeChangeRestricted(boolean pipeChangeRestricted) {
        this.pipeChangeRestricted = pipeChangeRestricted;
    }

    public static DataTransferSettings loadSettings(DBRRunnableWithResult<DataTransferSettings> loader) throws DBException {
        // Wait 1 minute maximum
        RuntimeUtils.runTask(loader, "Load data transfer settings", 60000, false);
        DataTransferSettings settings = loader.getResult();
        if (settings == null) {
            throw new DBException("Timeout while loading data transfer settings");
        }
        return settings;
    }

    private void initializePipes(
        @Nullable Collection<? extends IDataTransferProducer> producers,
        @Nullable Collection<? extends IDataTransferConsumer> consumers,
        boolean isExport)
    {
        this.initProducers = producers == null ? null : producers.toArray(new IDataTransferProducer[0]);
        this.initConsumers = consumers == null ? null : consumers.toArray(new IDataTransferConsumer[0]);
        this.dataPipes = new ArrayList<>();
        this.initObjects.clear();
        this.consumerOptional = false;
        this.producerOptional = false;

        DataTransferRegistry registry = DataTransferRegistry.getInstance();

        if (!ArrayUtils.isEmpty(initProducers) && !ArrayUtils.isEmpty(initConsumers)) {
            // Both producers and consumers specified
            // Processor belongs to non-database nodes anyway
            if (initProducers.length != initConsumers.length) {
                // Something went wrong
                if (!isTaskRunning && initProducers.length < initConsumers.length && initProducers[0] instanceof DatabaseTransferProducer) {
                    // In this case, we had data transfer from table(s) to a file or another container.
                    // But, probably, table(s) were deleted, and we lost our producer(s).
                    // Usually, consumers do not have any special info, so we can delete extra items.
                    // To show task wizard to the user.
                    initConsumers = Arrays.copyOf(initConsumers, initProducers.length);
                } else {
                    throw new IllegalArgumentException("Producers number must match consumers number"
                        + (isExport ? ". Please check your source containers" : ""));
                }
            }
            // Make pipes
            for (int i = 0; i < initProducers.length; i++) {
                if (initProducers[i].getDatabaseObject() != null) initObjects.add(initProducers[i].getDatabaseObject());
                dataPipes.add(new DataTransferPipe(initProducers[i], initConsumers[i]));
            }
            consumerOptional = isExport;//initProducers[0] instanceof IDataTransferNodePrimary;
            producerOptional = !isExport;//initConsumers[0] instanceof IDataTransferNodePrimary;
            if (producerOptional && consumerOptional) {
                // Both producer and consumer set are primary
                // This may happen when task was saved for db-> settings
                // Leave only optional consumer then
                producerOptional = false;
            }
        } else if (!ArrayUtils.isEmpty(initProducers)) {
            // Make pipes
            for (IDataTransferProducer<?> source : initProducers) {
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
                DBWorkbench.getPlatformUI().showError(DTMessages.data_transfer_settings_title_find_producer, DTMessages.data_transfer_settings_message_find_data_producer);
            }
        } else if (!ArrayUtils.isEmpty(initConsumers)) {
            // Make pipes
            for (IDataTransferConsumer<?,?> target : initConsumers) {
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
                DBWorkbench.getPlatformUI().showError(
                    DTMessages.data_transfer_settings_title_find_consumer,
                    DTMessages.data_transfer_settings_message_find_data_consumer);
            }
            producerOptional = true;
        } else {
            consumerOptional = true;
            producerOptional = true;
        }

        if (!ArrayUtils.isEmpty(initConsumers)) {
            for (IDataTransferConsumer<?,?> target : initConsumers) {
                DataTransferNodeDescriptor node = registry.getNodeByType(target.getClass());
                if (node != null) {
                    this.consumer = node;
                }
            }
        }
    }

    public void loadSettings(Map<String, Object> config) {
        this.setMaxJobCount(CommonUtils.toInt(config.get("maxJobCount"), DataTransferSettings.DEFAULT_THREADS_NUM));
        this.setShowFinalMessage(CommonUtils.getBoolean(config.get("showFinalMessage"), this.isShowFinalMessage()));

        DataTransferNodeDescriptor savedConsumer = null, savedProducer = null, processorNode = null;
        {
            // Restore consumer/producer from saved configuration
            // Do this only if consumer/producer weren't set explicitly
            {
                String consumerId = CommonUtils.toString(config.get("consumer"));
                if (!CommonUtils.isEmpty(consumerId)) {
                    DataTransferNodeDescriptor consumerNode = DataTransferRegistry.getInstance().getNodeById(consumerId);

                    // Check that this consumer is allowed
                    if (!CommonUtils.isEmpty(initObjects)) {
                        if (!DataTransferRegistry.getInstance().getAvailableConsumers(initObjects).contains(consumerNode)) {
                            consumerNode = null;
                        }
                    }
                    if (consumerNode != null) {
                        if (this.consumer == null){
                            savedConsumer = consumerNode;
                            this.setConsumer(consumerNode);
                        } else {
                            savedConsumer = this.consumer;
                        }
                        if (consumerNode.hasProcessors()) {
                            processorNode = consumerNode;
                        }
                    }
                }
            }
            {
                String producerId = CommonUtils.toString(config.get("producer"));
                if (!CommonUtils.isEmpty(producerId)) {
                    DataTransferNodeDescriptor producerNode = DataTransferRegistry.getInstance().getNodeById(producerId);
                    // Check that this producer is allowed
                    if (!CommonUtils.isEmpty(initObjects)) {
                        if (!DataTransferRegistry.getInstance().getAvailableProducers(initObjects).contains(producerNode)) {
                            producerNode = null;
                        }
                    }

                    if (producerNode != null) {
                        if (this.producer == null) {
                            savedProducer = producerNode;
                            this.setProducer(producerNode);
                        } else {
                            savedProducer = this.producer;
                        }
                        if (producerNode.hasProcessors()) {
                            processorNode = producerNode;
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
                if (savedProcessor == null) {
                    state.addError(new DBException("Processor '" + processorId + "' not found in '" + processorNode.getName() + "'"));
                }
            }
        }
        if (this.consumerOptional && savedConsumer != null) {
            this.selectConsumer(savedConsumer, savedProcessor, false);
        }
        if (this.producerOptional && savedProducer != null) {
            this.selectProducer(savedProducer, savedProcessor, false);
        }

        if (processorNode == consumer) {
            producerProcessor = false;
        } else {
            producerProcessor = true;
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
                    Map<String, Object> props = new HashMap<>();
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
    }

    public boolean isNodeSettingsLoaded() {
        return nodeSettingsLoaded;
    }

    public void loadNodeSettings(DBRProgressMonitor monitor) {
        if (nodeSettingsLoaded) {
            return;
        }
        // Load nodes' settings (key is impl class simple name, value is descriptor)
        Map<String, DataTransferNodeDescriptor> nodeNames = new LinkedHashMap<>();
        if (producer != null) {
            nodeNames.put(producer.getNodeClass().getSimpleName(), producer);
        }
        if (consumer != null) {
            nodeNames.put(consumer.getNodeClass().getSimpleName(), consumer);
        }

        MonitorRunnableContext runnableContext = new MonitorRunnableContext(monitor);
        for (Map.Entry<String, DataTransferNodeDescriptor> node : nodeNames.entrySet()) {
            Map<String, Object> nodeSection = JSONUtils.getObject(configurationMap, node.getKey());
            IDataTransferSettings nodeSettings = this.getNodeSettings(node.getValue());
            if (nodeSettings != null) {
                nodeSettings.loadSettings(runnableContext, this, nodeSection);
            }
        }

        // Initialize pipes with loaded settings
        for (int i = 0; i < dataPipes.size(); i++) {
            DataTransferPipe pipe = dataPipes.get(i);
            if (pipe.getProducer() != null && pipe.getConsumer() != null) {
                try {
                    pipe.initPipe(this, i, dataPipes.size());
                } catch (Exception e) {
                    state.addError(e);
                }
            }
        }

        this.nodeSettingsLoaded = true;
    }

    public boolean isConsumerOptional() {
        return consumerOptional;
    }

    public boolean isProducerOptional() {
        return producerOptional;
    }

    public IDataTransferProducer<?>[] getInitProducers() {
        return initProducers;
    }

    @Nullable
    public IDataTransferConsumer<?,?>[] getInitConsumers() {
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

    public Map<DataTransferProcessorDescriptor, Map<String, Object>> getProcessorPropsHistory() {
        return processorPropsHistory;
    }

    public Map<String, Object> getProcessorProperties() {
        if (processor == null) {
            log.debug("No processor selected - no properties");
            return null;
        }
        return processorPropsHistory.get(processor);
    }

    public void setProcessorProperties(Map<String, Object> properties) {
        if (processor == null) {
            throw new IllegalStateException("No processor selected");
        }
        processorPropsHistory.put(processor, properties);
    }

    public List<DataTransferPipe> getDataPipes() {
        return dataPipes;
    }

    public void sortDataPipes(DBRProgressMonitor monitor) {
        List<DBSEntity> entities = dataPipes.stream().sequential()
                .filter(pipe -> pipe.getProducer() != null && pipe.getProducer().getDatabaseObject() instanceof DBSEntity)
                .map(pipe -> (DBSEntity) pipe.getProducer().getDatabaseObject())
                .collect(Collectors.toList());
        List<DBSEntity> simpleTables = new ArrayList<>();
        List<DBSEntity> cyclicTables = new ArrayList<>();
        List<DBSEntity> views = new ArrayList<>();
        try {
            DBStructUtils.sortTableList(monitor, entities, simpleTables, cyclicTables, views);
        } catch (DBException e) {
            log.warn("Unable to sort database entities!");
            return;
        }
        dataPipes.sort((pipe1, pipe2) -> { //fixme rewrite
            IDataTransferProducer<?> producer1 = pipe1.getProducer();
            IDataTransferProducer<?> producer2 = pipe2.getProducer();
            if (producer1 == null && producer2 == null) {
                return 0;
            } else if (producer1 == null) {
                return 1;
            } else if (producer2 == null) {
                return -1;
            }
            DBSObject dbsObject1 = producer1.getDatabaseObject();
            DBSObject dbsObject2 = producer2.getDatabaseObject();
            if (dbsObject1 == null && dbsObject2 == null) {
                return 0;
            } else if (dbsObject1 == null) {
                return 1;
            } else if (dbsObject2 == null) {
                return -1;
            }
            if (!(dbsObject1 instanceof DBSEntity) && !(dbsObject2 instanceof DBSEntity)) {
                return 0;
            } else if (!(dbsObject1 instanceof DBSEntity)) {
                return 1;
            } else if (!(dbsObject2 instanceof DBSEntity)) {
                return -1;
            }
            DBSEntity entity1 = (DBSEntity) dbsObject1;
            DBSEntity entity2 = (DBSEntity) dbsObject2;
            int idx1 = views.indexOf(entity1);
            int idx2 = views.indexOf(entity2);
            if (idx1 != -1 || idx2 != -1) {
                return idx1 - idx2;
            }
            idx1 = cyclicTables.indexOf(entity1);
            idx2 = cyclicTables.indexOf(entity2);
            if (idx1 != -1 || idx2 != -1) {
                return idx1 - idx2;
            }
            return simpleTables.indexOf(entity1) - simpleTables.indexOf(entity2);
        });
    }

    public void processPipeEarlier(@NotNull DataTransferPipe pipe) {
        CommonUtils.shiftLeft(dataPipes, pipe);
    }

    public void processPipeLater(@NotNull DataTransferPipe pipe) {
        CommonUtils.shiftRight(dataPipes, pipe);
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
                    IDataTransferConsumer<?,?> consumerNode = (IDataTransferConsumer<?,?>) consumer.createNode();
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
                    pipe.setProducer((IDataTransferProducer<?>) producer.createNode());
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

    public static void saveNodesLocation(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state, Collection<IDataTransferNode<?>> nodes, String nodeType) {
        if (nodes != null) {
            List<Map<String, Object>> inputObjects = new ArrayList<>();
            for (Object inputObject : nodes) {
                inputObjects.add(JSONUtils.serializeObject(runnableContext, task, inputObject));
            }
            state.put(nodeType, inputObjects);
        }
    }

    public static <T> List<T> getNodesFromLocation(@NotNull DBRProgressMonitor monitor, DBTTask task, DataTransferState state, Log taskLog, String nodeType, Class<T> nodeClass) {
        Map<String, Object> config = task.getProperties();
        List<T> result = new ArrayList<>();
        Object nodeList = config.get(nodeType);
        if (nodeList instanceof Collection) {
            MonitorRunnableContext runnableContext = new MonitorRunnableContext(monitor);
            for (Object nodeObj : (Collection<?>)nodeList) {
                if (nodeObj instanceof Map) {
                    try {
                        Object node = JSONUtils.deserializeObject(runnableContext, task, (Map<String, Object>) nodeObj);
                        if (nodeClass.isInstance(node)) {
                            result.add(nodeClass.cast(node));
                        }
                    } catch (DBCException e) {
                        state.addError(e);
                        taskLog.error(e);
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

    public void setDataPipes(List<DataTransferPipe> dataPipes, boolean isExport) {
        this.dataPipes = dataPipes;

        // Now determine main producer and consumer and processor
        DataTransferRegistry registry = DataTransferRegistry.getInstance();

        this.consumerOptional = isExport;
        this.producerOptional = !isExport;

        // Don't update producer and consumer if it's not required (#9687)
        if (!nodeUpdateRestricted) {
            this.producer = null;
            this.consumer = null;
            if (!dataPipes.isEmpty()) {
                DataTransferPipe pipe = dataPipes.get(0);
                this.producer = pipe.getProducer() == null ? null : registry.getNodeByType(pipe.getProducer().getClass());
                this.consumer = pipe.getConsumer() == null ? null : registry.getNodeByType(pipe.getConsumer().getClass());
            }
        }

        DataTransferProcessorDescriptor savedProcessor = this.processor;
        if (this.consumerOptional && this.consumer != null) {
            this.selectConsumer(this.consumer, savedProcessor, false);
        }
        if (this.producerOptional && this.producer != null) {
            this.selectProducer(this.producer, savedProcessor, false);
        }

        // Collect objects
        initObjects.clear();
        for (DataTransferPipe pipe : dataPipes) {
            DBSObject object = isExport ? pipe.getProducer().getDatabaseObject() : pipe.getConsumer().getDatabaseObject();
            if (object != null) {
                initObjects.add(object);
            }
        }
    }
}
