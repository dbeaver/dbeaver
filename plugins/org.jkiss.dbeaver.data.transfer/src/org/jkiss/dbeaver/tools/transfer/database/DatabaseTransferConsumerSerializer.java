package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DatabaseTransferConsumerSerializer implements DBPObjectSerializer<DBTTask, DatabaseTransferConsumer> {

    private static final Log log = Log.getLog(DatabaseTransferConsumerSerializer.class);

    @Override
    public void serializeObject(DBRRunnableContext runnableContext, DBTTask context, DatabaseTransferConsumer object, Map<String, Object> state) {
        try {
            runnableContext.run(true, true, monitor -> {
                try {
                    object.checkTargetContainer(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });

            DBPDataSourceContainer targetDS = object.getDataSourceContainer();
            if (targetDS == null) {
                throw new DBException("Can't get target datasource container");
            }
            state.put("type", "mappings");
            state.put("project", targetDS.getProject().getName());
            state.put("dataSource", targetDS.getId());

            DBSDataContainer dataContainer = object.getTargetObject();
            if (dataContainer instanceof DBSEntity) {
                state.put("entityId", DBUtils.getObjectFullId(dataContainer));
            }

            // Save mappings
            DatabaseMappingContainer containerMapping = object.getContainerMapping();
            if (containerMapping != null) {
                Map<String, Object> cmMap = new LinkedHashMap<>();
                state.put("mapping", cmMap);
                cmMap.put("type", containerMapping.getMappingType().name());
                if (dataContainer == null) {
                    DBSObjectContainer targetContainer = containerMapping.getSettings().getContainer();
                    if (targetContainer != null) {
                        state.put("targetContainerId", DBUtils.getObjectFullId(targetContainer));
                    }
                    cmMap.put("targetName", containerMapping.getTargetName());
                    Collection<DatabaseMappingAttribute> attributeMappings = containerMapping.getAttributeMappings(runnableContext);
                    if (!CommonUtils.isEmpty(attributeMappings)) {
                        List<Map<String, Object>> amList = new ArrayList<>();
                        cmMap.put("attributes", amList);
                        for (DatabaseMappingAttribute am : attributeMappings) {
                            Map<String, Object> amMap = new LinkedHashMap<>();
                            amList.add(amMap);
                            amMap.put("type", am.getMappingType().name());
                            amMap.put("source", am.getSource().getName());
                            amMap.put("target", am.getTargetName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error initializing database consumer", e);
        }
    }

    @Override
    public DatabaseTransferConsumer deserializeObject(DBRRunnableContext runnableContext, DBTTask objectContext, Map<String, Object> state) throws DBCException {
        DatabaseTransferConsumer consumer = new DatabaseTransferConsumer();

        String entityId = CommonUtils.toString(state.get("entityId"), null);
        if (entityId != null) {
            try {
                runnableContext.run(false, true, monitor -> {
                    try {
                        String projectName = CommonUtils.toString(state.get("project"));
                        DBPProject project = CommonUtils.isEmpty(projectName) ? null : DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
                        if (project == null) {
                            throw new DBCException("Project '" + projectName + "' not found");
                        }
                        //consumer.targetObject = (DBSDataManipulator) DBUtils.findObjectById(monitor, project, entityId);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (InvocationTargetException e) {
                throw new DBCException("Error instantiating data consumer", e.getTargetException());
            } catch (InterruptedException e) {
                throw new DBCException("Deserialization canceled", e);
            }
        }

        return consumer;
    }
}
