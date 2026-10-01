/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.erd;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class ERDUtils {
    private static final Log log = Log.getLog(ERDUtils.class);

    @NotNull
    public static Collection<? extends DBSEntityAttribute> getBestTableIdentifier(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity entity
    ) throws DBException {
        if (entity instanceof DBSTable table && table.isView()) {
            return Collections.emptyList();
        }
        if (CommonUtils.isEmpty(entity.getAttributes(monitor))) {
            return Collections.emptyList();
        }

        // Find PK or unique key
        DBSEntityConstraint uniqueId = null;
        for (DBSEntityConstraint id : CommonUtils.safeCollection(entity.getConstraints(monitor))) {
            if (id instanceof DBSEntityReferrer entityReferrer && id.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                return DBUtils.getEntityAttributes(monitor, entityReferrer);
            } else if (id.getConstraintType().isUnique()) {
                uniqueId = id;
            } else if (id instanceof DBSTableIndex index && index.isUnique()) {
                uniqueId = id;
            }
        }
        if (uniqueId instanceof DBSEntityReferrer idReferrer) {
            return DBUtils.getEntityAttributes(monitor, idReferrer);
        }

        // Check indexes
        if (entity instanceof DBSTable table) {
            try {
                Collection<? extends DBSTableIndex> indexes = table.getIndexes(monitor);
                if (!CommonUtils.isEmpty(indexes)) {
                    for (DBSTableIndex index : indexes) {
                        if (DBUtils.isIdentifierIndex(monitor, index)) {
                            return DBUtils.getEntityAttributes(monitor, index);
                        }
                    }
                }
            } catch (DBException e) {
                log.debug(e);
            }
        }
        return Collections.emptyList();
    }

    public static boolean isIdentifyingAssociation(@NotNull ERDAssociation association) {
        if (association.isLogical()) {
            return false;
        }
        try {
            return DBUtils.isIdentifyingAssociation(new VoidProgressMonitor(), association.getObject());
        } catch (DBException e) {
            log.debug(e);
            return false;
        }
    }

    public static boolean isOptionalAssociation(@NotNull ERDAssociation association) {
        try {
            return DBUtils.isOptionalAssociation(new VoidProgressMonitor(), association.getObject());
        } catch (DBException e) {
            log.debug(e);
            return false;
        }
    }

    /**
     * The method designed to find attributes by name comparison for one entity
     *
     * @param entity - database entity (table)
     * @param attr - attribute 
     * @return - existing attribute or null
     */
    @Nullable
    public static ERDEntityAttribute getAttributeByModel(@Nullable ERDEntity entity, @NotNull DBSEntityAttribute attr) {
        if (entity == null) {
            return null;
        }
        for (ERDEntityAttribute erdAttr : entity.getAttributes()) {
            if (erdAttr.getObject().getName().equals(attr.getName())) {
                return erdAttr;
            }
        }
        return null;
    }

    @Nullable
    public static <T> List<T> getObjectsFromERD(@Nullable List<? extends ERDObject<T>> erdObjects) {
        List<T> result = null;
        if (erdObjects != null) {
            result = new ArrayList<>();
            for (ERDObject<T> erdObject : erdObjects) {
                result.add(erdObject.getObject());
            }
        }
        return result;
    }

    @Nullable
    public static ERDEntity makeEntityFromObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull ERDDiagram diagram,
        @NotNull List<ERDEntity> otherEntities,
        @NotNull DBSEntity entity,
        @Nullable Object userData
    ) {
        ERDEntity erdEntity = new ERDEntity(entity);
        erdEntity.setUserData(userData);
        try {
            diagram.getContentProvider().fillEntityFromObject(monitor, diagram, otherEntities, erdEntity);
        } catch (DBCException e) {
            log.error("Can't create ERD entity from database entity " + entity, e);
            return null;
        }
        return erdEntity;
    }

    @NotNull
    public static Collection<DBSEntity> collectDatabaseTables(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject root,
        @NotNull ERDDiagram diagram,
        boolean showViews,
        boolean showPartitions
    ) throws DBException {
        Set<DBSEntity> result = new LinkedHashSet<>();

        // Cache structure
        if (root instanceof DBSObjectContainer objectContainer) {
            monitor.beginTask("Load '" + root.getName() + "' content", 3);
            try {
                DBExecUtils.tryExecuteRecover(monitor, objectContainer.getDataSource(), param ->
                    objectContainer.cacheStructure(
                        monitor,
                        DBSObjectContainer.STRUCT_ENTITIES
                            | DBSObjectContainer.STRUCT_ASSOCIATIONS
                            | DBSObjectContainer.STRUCT_ATTRIBUTES));
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Cache database model", "Error caching database model", e);
            }
            Collection<? extends DBSObject> entities = objectContainer.getChildren(monitor);
            if (entities != null) {
                Class<? extends DBSObject> childType = objectContainer.getPrimaryChildType(monitor);
                DBSObjectFilter objectFilter = objectContainer.getDataSource().getContainer()
                    .getObjectFilter(childType, objectContainer, true);

                for (DBSObject entity : entities) {
                    if (entity instanceof DBSEntity dbsEntity) {
                        if (objectFilter != null && objectFilter.isEnabled() && !objectFilter.matches(entity.getName())) {
                            continue;
                        }

                        if (skipSystemEntity(dbsEntity)) {
                            continue;
                        }

                        if (dbsEntity.getEntityType() == DBSEntityType.TABLE ||
                            dbsEntity.getEntityType() == DBSEntityType.CLASS ||
                            dbsEntity.getEntityType() == DBSEntityType.VIRTUAL_ENTITY ||
                            (showViews && DBUtils.isView(dbsEntity))
                        ) {
                            result.add(dbsEntity);
                        }
                    }
                }
            }
            monitor.done();

        } else if (root instanceof DBSEntity rootEntity) {
            monitor.beginTask("Load '" + root.getName() + "' relations", 3);
            result.add(rootEntity);
            try {
                monitor.subTask("Read foreign keys");
                Collection<? extends DBSEntityAssociation> fks = DBVUtils.getAllAssociations(monitor, rootEntity);
                for (DBSEntityAssociation fk : fks) {
                    DBSEntity associatedEntity = fk.getAssociatedEntity();
                    if (associatedEntity != null) {
                        result.add(DBVUtils.getRealEntity(monitor, associatedEntity));
                    }
                }
                monitor.worked(1);
            } catch (DBException e) {
                log.warn("Can't load table foreign keys", e);
            }
            if (monitor.isCanceled()) {
                return result;
            }
            monitor.subTask("Read references");
            Collection<? extends DBSEntityAssociation> refs = DBVUtils.getAllReferences(monitor, rootEntity);
            for (DBSEntityAssociation ref : refs) {
                result.add(ref.getParentObject());
            }

            monitor.worked(1);
            if (monitor.isCanceled()) {
                return result;
            }
            try {
                monitor.subTask("Read associations");
                List<DBSEntity> secondLevelEntities = new ArrayList<>();
                for (DBSEntity entity : result) {
                    if (entity != rootEntity && entity.getEntityType() == DBSEntityType.ASSOCIATION) {
                        // Read all association's associations
                        Collection<? extends DBSEntityAssociation> fks = entity.getAssociations(monitor);
                        if (fks != null) {
                            for (DBSEntityAssociation association : fks) {
                                if (association.getConstraintType() != DBSEntityConstraintType.INHERITANCE) {
                                    secondLevelEntities.add(association.getAssociatedEntity());
                                }
                            }
                        }
                    }
                }
                result.addAll(secondLevelEntities);
                monitor.worked(1);
            } catch (DBException e) {
                log.warn("Can't load table references", e);
            }

            monitor.done();
        }

        // Remove entities already loaded in the diagram
        for (ERDEntity diagramEntity : diagram.getEntities()) {
            result.remove(diagramEntity.getObject());
        }

        if (!showPartitions) {
            result.removeIf(entity -> entity instanceof DBSTablePartition);
        }

        return result;
    }

    public static boolean skipSystemEntity(@NotNull DBSEntity entity) {
        boolean showSystemObjects = entity.getDataSource().getContainer().getNavigatorSettings().isShowSystemObjects();
        return !showSystemObjects && entity instanceof DBPSystemObject && ((DBPSystemObject) entity).isSystem();
    }
}
