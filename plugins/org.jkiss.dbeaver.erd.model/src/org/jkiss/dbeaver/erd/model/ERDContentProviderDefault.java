/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPObjectWithLazyDescription;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * ERD content provider
 */
public class ERDContentProviderDefault implements ERDContentProvider {

    private static final Log log = Log.getLog(ERDContentProviderDefault.class);

    private static final boolean READ_LAZY_DESCRIPTIONS = false;

    private final Map<String, Object> attributes = new HashMap<>();

    public ERDContentProviderDefault() {
    }

    @Override
    public boolean allowEntityDuplicates() {
        return false;
    }

    @Override
    public void fillEntityFromObject(@NotNull DBRProgressMonitor monitor, @NotNull ERDDiagram diagram,
                                     @NotNull List<ERDEntity> otherEntities, @NotNull ERDEntity erdEntity) throws DBCException {
        fillEntityFromObject(monitor, diagram, otherEntities, erdEntity, new ERDAttributeSettings(ERDAttributeVisibility.ALL, false));
    }

    @Override
    public void fillEntityFromObject(@NotNull DBRProgressMonitor monitor, @NotNull ERDDiagram diagram, @NotNull List<ERDEntity> otherEntities, @NotNull ERDEntity erdEntity, @NotNull ERDAttributeSettings settings) {
        DBSEntity entity = erdEntity.getObject();
        if (READ_LAZY_DESCRIPTIONS && entity instanceof DBPObjectWithLazyDescription) {
            try {
                ((DBPObjectWithLazyDescription) entity).getDescription(monitor);
            } catch (DBException e) {
                log.warn("Unable to load lazy description when filling ERDEntity from object");
            }
        }
        if (settings.getVisibility() != ERDAttributeVisibility.NONE) {
            Set<DBSEntityAttribute> keyColumns = new HashSet<>();
            try {
                for (DBSEntityAssociation assoc : DBVUtils.getAllAssociations(monitor, entity)) {
                    if (assoc instanceof DBSEntityReferrer) {
                        keyColumns.addAll(DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) assoc));
                    }
                }
                for (DBSEntityConstraint constraint : DBVUtils.getAllConstraints(monitor, entity)) {
                    if (constraint instanceof DBSEntityReferrer) {
                        keyColumns.addAll(DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) constraint));
                    }
                }
            } catch (DBException e) {
                log.warn(e);
            }

            Collection<? extends DBSEntityAttribute> idColumns = null;
            try {
                idColumns = ERDUtils.getBestTableIdentifier(monitor, entity);
                keyColumns.addAll(idColumns);
            } catch (DBException e) {
                log.error("Error reading table identifier", e);
            }
            try {
                boolean attrNodesCached = false;
                List<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
                if (CommonUtils.isEmpty(attributes)) {
                    return;
                }
                DBSEntityAttribute firstAttr = attributes.iterator().next();
                DBSObjectFilter columnFilter = entity.getDataSource().getContainer().getObjectFilter(firstAttr.getClass(), entity, false);
                for (int i = 0; i < attributes.size(); i++) {
                    DBSEntityAttribute attribute = attributes.get(i);
                    boolean isInIdentifier = idColumns != null && idColumns.contains(attribute);
                    if (!isAttributeVisible(erdEntity, attribute)) {
                        // Show only visible attributes
                        continue;
                    }
                    if (columnFilter != null && !columnFilter.matches(attribute.getName())) {
                        continue;
                    }
                    if (!attrNodesCached) {
                        // Pre-load navigator node as well.
                        // It may be needed later because all ERD objects can be adapted to navigator
                        // nodes.
                        DBNUtils.getNodeByObject(monitor, attribute, false);
                        attrNodesCached = true;
                    }

                    switch (settings.getVisibility()) {
                        case PRIMARY:
                            if (!isInIdentifier) {
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
                    ERDEntityAttribute c1 = new ERDEntityAttribute(attribute, inPrimaryKey);
                    erdEntity.addAttribute(c1, false);
                }
                if (settings.isAlphabeticalOrder()) {
                    erdEntity.sortAttributes(DBUtils.nameComparatorIgnoreCase(), false);
                }
            } catch (DBException e) {
                // just skip this problematic attributes
                log.debug("Can't load table '" + entity.getName() + "'attributes", e);
            }
        }
    }

    protected boolean isAttributeVisible(ERDEntity erdEntity, DBSEntityAttribute attribute) {
        if (attribute instanceof DBSEntityAssociation) {
            // skip attributes which are associations
            // usual thing in some systems like WMI/CIM model
            return false;
        }
        if (DBUtils.isHiddenObject(attribute) || DBUtils.isInheritedObject(attribute)) {
            // Skip hidden attributes
            return false;
        }
        return true;
    }

    @Override
    public ERDAssociation createAutoAssociation(ERDContainer diagram, 
        @NotNull DBSEntityAssociation association,
        @NotNull ERDEntity sourceEntity, 
        @NotNull ERDEntity targetEntity,
        boolean reflect) {
        // Allow all auto-associations
        ERDAssociation erdAssociation = new ERDAssociation(association, sourceEntity, targetEntity, reflect);
        erdAssociation.resolveAttributes();
        return erdAssociation;
    }

    @Override
    public ERDAssociation createAssociation(ERDContainer diagram,
        DBSEntityAssociation association,
        ERDEntity sourceEntity,
        ERDEntityAttribute sourceAttribute,
        ERDEntity targetEntity,
        ERDEntityAttribute targetAttribute, boolean reflect) {
        ERDAssociation erdAssociation = new ERDAssociation(association, sourceEntity, targetEntity, reflect);
        erdAssociation.addCondition(sourceAttribute, targetAttribute);
        return erdAssociation;
    }

    @Override
    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }
}
