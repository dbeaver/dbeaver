/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ERD content provider
 */
public class ERDContentProviderDefault implements ERDContentProvider {

    private static final Log log = Log.getLog(ERDContentProviderDefault.class);

    public ERDContentProviderDefault() {
    }

    @Override
    public boolean allowEntityDuplicates() {
        return false;
    }

    @Override
    public void fillEntityFromObject(@NotNull DBRProgressMonitor monitor, @NotNull ERDDiagram diagram, List<ERDEntity> otherEntities, @NotNull ERDEntity erdEntity) {
        ERDAttributeVisibility attributeVisibility = ERDAttributeVisibility.ALL;
        fillEntityFromObject(monitor, erdEntity, attributeVisibility);
    }

    protected void fillEntityFromObject(@NotNull DBRProgressMonitor monitor, ERDEntity erdEntity, ERDAttributeVisibility attributeVisibility) {
        DBSEntity entity = erdEntity.getObject();
        if (attributeVisibility != ERDAttributeVisibility.NONE) {
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

                Collection<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
                DBSEntityAttribute firstAttr = CommonUtils.isEmpty(attributes) ? null : attributes.iterator().next();
                DBSObjectFilter columnFilter = firstAttr == null ? null :
                    entity.getDataSource().getContainer().getObjectFilter(firstAttr.getClass(), entity, false);
                if (!CommonUtils.isEmpty(attributes)) {
                    for (DBSEntityAttribute attribute : attributes) {
                        boolean isInIdentifier = idColumns != null && idColumns.contains(attribute);
                        if (!keyColumns.contains(attribute) && !isAttributeVisible(erdEntity, attribute)) {
                            // Show all visible attributes and all key attributes
                            continue;
                        }
                        if (columnFilter != null && !columnFilter.matches(attribute.getName())) {
                            continue;
                        }

                        switch (attributeVisibility) {
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
    public ERDAssociation createAutoAssociation(ERDContainer diagram, @NotNull DBSEntityAssociation association, @NotNull ERDEntity sourceEntity, @NotNull ERDEntity targetEntity, boolean reflect) {
        // Allow all auto-associations
        return new ERDAssociation(association, sourceEntity, targetEntity, reflect);
    }

}
