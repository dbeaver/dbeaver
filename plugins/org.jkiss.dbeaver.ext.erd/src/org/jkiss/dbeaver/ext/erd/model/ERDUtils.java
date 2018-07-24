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

public class ERDUtils
{
    private static final Log log = Log.getLog(ERDUtils.class);

    public static String getFullAttributeLabel(EntityDiagram diagram, ERDEntityAttribute attribute, boolean includeType) {
        String attributeLabel = attribute.getName();
        if (includeType && diagram.hasAttributeStyle(ERDViewStyle.TYPES)) {
            attributeLabel += ": " + attribute.getObject().getFullTypeName();
        }
        if (includeType && diagram.hasAttributeStyle(ERDViewStyle.NULLABILITY)) {
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

    public static ERDEntity makeEntityFromObject(DBRProgressMonitor monitor, EntityDiagram diagram, DBSEntity entity, Object userData) {
        ERDEntity erdEntity = new ERDEntity(entity);
        erdEntity.setUserData(userData);
        diagram.getDecorator().fillEntityFromObject(monitor, diagram, erdEntity);
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

    public static ERDEntityAttribute getAttributeByModel(ERDEntity entity, DBSEntityAttribute attr) {
	    for (ERDEntityAttribute erdAttr : entity.getAttributes()) {
	        if (erdAttr.getObject() == attr) {
	            return erdAttr;
            }
        }
        return null;
    }
}
