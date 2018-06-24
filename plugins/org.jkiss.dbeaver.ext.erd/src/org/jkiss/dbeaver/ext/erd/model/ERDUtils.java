/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.editor.ERDAttributeVisibility;
import org.jkiss.dbeaver.ext.erd.editor.ERDViewStyle;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ERDUtils
{
    private static final Log log = Log.getLog(ERDUtils.class);

    public static String getAttributeLabel(EntityDiagram diagram, ERDEntityAttribute attribute) {
        String attributeLabel;
        if (diagram.hasAttributeStyle(ERDViewStyle.TYPES)) {
            attributeLabel = attribute.getName() + ": " + attribute.getObject().getFullTypeName();
        } else {
            attributeLabel = attribute.getName();
        }
        if (diagram.hasAttributeStyle(ERDViewStyle.NULLABILITY)) {
            if (attribute.getObject().isRequired()) {
                attributeLabel += " NOT NULL";
            }
        }
        if (diagram.hasAttributeStyle(ERDViewStyle.COMMENTS)) {
            String comment = attribute.getObject().getDescription();
            if (!CommonUtils.isEmpty(comment)) {
                attributeLabel += " - " + comment;
            }
        }
        return attributeLabel;
	}

    public static ERDEntity makeEntityFromObject(DBRProgressMonitor monitor, EntityDiagram diagram, DBSEntity entity) {
        ERDEntity erdEntity = new ERDEntity(entity);
        ERDAttributeVisibility attributeVisibility = diagram.getAttributeVisibility();
        if (attributeVisibility != ERDAttributeVisibility.NONE) {
            Set<DBSEntityAttribute> keyColumns = null;
            if (attributeVisibility == ERDAttributeVisibility.KEYS) {
                keyColumns = new HashSet<>();
                try {
                    for (DBSEntityAssociation assoc : CommonUtils.safeCollection(entity.getAssociations(monitor))) {
                        if (assoc instanceof DBSEntityReferrer) {
                            keyColumns.addAll(DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) assoc));
                        }
                    }
                    for (DBSEntityConstraint constraint : CommonUtils.safeCollection(entity.getConstraints(monitor))) {
                        if (constraint instanceof DBSEntityReferrer) {
                            keyColumns.addAll(DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) constraint));
                        }
                    }
                } catch (DBException e) {
                    log.warn(e);
                }
            }
            Collection<? extends DBSEntityAttribute> idColumns = null;
            try {
                idColumns = getBestTableIdentifier(monitor, entity);
                if (keyColumns != null) {
                    keyColumns.addAll(idColumns);
                }
            } catch (DBException e) {
                log.error("Error reading table identifier", e);
            }
            try {

                DBSObjectFilter columnFilter = entity.getDataSource().getContainer().getObjectFilter(DBSEntityAttribute.class, entity, false);
                Collection<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
                if (!CommonUtils.isEmpty(attributes)) {
                    for (DBSEntityAttribute attribute : attributes) {
                        if (attribute instanceof DBSEntityAssociation) {
                            // skip attributes which are associations
                            // usual thing in some systems like WMI/CIM model
                            continue;
                        }
                        if (DBUtils.isHiddenObject(attribute)) {
                            // Skip hidden attributes
                            continue;
                        }
                        if (columnFilter != null && !columnFilter.matches(attribute.getName())) {
                            continue;
                        }

                        switch (attributeVisibility) {
                            case PRIMARY:
                                if (idColumns == null || !idColumns.contains(attribute)) {
                                    continue;
                                }
                                break;
                            case KEYS:
                                if (!keyColumns.contains(attribute)) {
                                    continue;
                                }
                                break;
                            default:
                                break;
                        }
                        boolean inPrimaryKey = idColumns != null && idColumns.contains(attribute);
                        ERDEntityAttribute c1 = new ERDEntityAttribute(erdEntity, attribute, inPrimaryKey);
                        erdEntity.addAttribute(c1, false);
                    }
                }
            } catch (DBException e) {
                // just skip this problematic attributes
                log.debug("Can't load table '" + entity.getName() + "'attributes", e);
            }
        }
        return erdEntity;
    }

    @NotNull
    public static Collection<? extends DBSEntityAttribute> getBestTableIdentifier(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity entity)
        throws DBException {
        if (entity instanceof DBSTable && ((DBSTable) entity).isView()) {
            return Collections.emptyList();
        }
        if (CommonUtils.isEmpty(entity.getAttributes(monitor))) {
            return Collections.emptyList();
        }

        // Find PK or unique key
        DBSEntityConstraint uniqueId = null;
        //DBSEntityConstraint uniqueIndex = null;
        for (DBSEntityConstraint id : CommonUtils.safeCollection(entity.getConstraints(monitor))) {
            if (id instanceof DBSEntityReferrer && id.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                return DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) id);
            } else if (id.getConstraintType().isUnique()) {
                uniqueId = id;
            } else if (id instanceof DBSTableIndex && ((DBSTableIndex) id).isUnique()) {
                uniqueId = id;
            }
        }
        if (uniqueId instanceof DBSEntityReferrer) {
            return DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) uniqueId);
        }

        // Check indexes
        if (entity instanceof DBSTable) {
            try {
                Collection<? extends DBSTableIndex> indexes = ((DBSTable) entity).getIndexes(monitor);
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

    public static boolean isIdentifyingAssociation(ERDAssociation association) {
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

    public static void openObjectEditor(@NotNull ERDObject object) {
        if (object.getObject() instanceof DBSObject) {
            UIUtils.runUIJob("Open object editor", monitor -> {
                DBNDatabaseNode node = NavigatorUtils.getNodeByObject(
                    monitor,
                    (DBSObject) object.getObject(),
                    true
                );
                if (node != null) {
                    NavigatorUtils.openNavigatorNode(node, UIUtils.getActiveWorkbenchWindow());
                }
            });
        }
    }

}
