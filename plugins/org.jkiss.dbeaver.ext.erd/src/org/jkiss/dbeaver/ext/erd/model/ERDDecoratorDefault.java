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
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.palette.*;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.editor.ERDAttributeVisibility;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditPartFactory;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ERD object adapter
 */
public class ERDDecoratorDefault implements ERDDecorator {

    public static final ImageDescriptor CONNECT_IMAGE = DBeaverIcons.getImageDescriptor(DBIcon.TREE_ASSOCIATION);
    public static final ImageDescriptor FOREIGN_KEY_IMAGE = DBeaverIcons.getImageDescriptor(DBIcon.TREE_FOREIGN_KEY);
    public static final ImageDescriptor NOTE_IMAGE = DBeaverIcons.getImageDescriptor(DBIcon.TYPE_TEXT);

    private static final Log log = Log.getLog(ERDDecoratorDefault.class);

    public ERDDecoratorDefault() {
    }

    @Override
    public boolean showCheckboxes() {
        return false;
    }

    @Override
    public boolean allowEntityDuplicates() {
        return false;
    }

    @Override
    public boolean supportsAttributeVisibility() {
        return true;
    }

    @Override
    public Insets getDefaultEntityInsets() {
        return new Insets(20, 20, 10, 20);
    }

    @Override
    public EditPartFactory createPartFactory() {
        return new ERDEditPartFactory();
    }

    @Override
    public void fillPalette(PaletteRoot paletteRoot, boolean readOnly) {
        // a group of default control tools
        PaletteDrawer controls = createToolsDrawer(paletteRoot);

        // the selection tool
        ToolEntry selectionTool = new SelectionToolEntry();
        controls.add(selectionTool);

        // use selection tool as default entry
        paletteRoot.setDefaultEntry(selectionTool);

        if (!readOnly) {
            // separator
            PaletteSeparator separator = new PaletteSeparator("tools");
            separator.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
            controls.add(separator);

            controls.add(new ConnectionCreationToolEntry(
                ERDMessages.erd_tool_create_connection,
                ERDMessages.erd_tool_create_connection_tip,
                null,
                CONNECT_IMAGE,
                CONNECT_IMAGE));
            controls.add(new CreationToolEntry(
                ERDMessages.erd_tool_create_note,
                ERDMessages.erd_tool_create_note_tip,
                new CreationFactory() {
                    @Override
                    public Object getNewObject()
                    {
                        return new ERDNote(ERDMessages.erd_tool_create_default);
                    }
                    @Override
                    public Object getObjectType()
                    {
                        return RequestConstants.REQ_CREATE;
                    }
                },
                NOTE_IMAGE,
                NOTE_IMAGE));
        }
    }

    protected PaletteDrawer createToolsDrawer(PaletteRoot paletteRoot) {
        PaletteDrawer controls = new PaletteDrawer("Tools", DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));

        paletteRoot.add(controls);

        return controls;
    }

    @Override
    public void fillEntityFromObject(DBRProgressMonitor monitor, EntityDiagram diagram, List<ERDEntity> otherEntities, ERDEntity erdEntity) {
        DBSEntity entity = erdEntity.getObject();
        ERDAttributeVisibility attributeVisibility = diagram.getDecorator().supportsAttributeVisibility() ?
            erdEntity.getAttributeVisibility() : ERDAttributeVisibility.ALL;
        if (attributeVisibility == null) {
            EntityDiagram.NodeVisualInfo visualInfo = diagram.getVisualInfo(erdEntity.getObject());
            if (visualInfo != null) {
                attributeVisibility = visualInfo.attributeVisibility;
            }
            if (attributeVisibility == null) {
                attributeVisibility = diagram.getAttributeVisibility();
            }
        }
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
    public ERDAssociation createAutoAssociation(ERDContainer diagram, DBSEntityAssociation association, ERDEntity sourceEntity, ERDEntity targetEntity, boolean reflect) {
        // Allow all auto-associations
        return new ERDAssociation(association, sourceEntity, targetEntity, reflect);
    }

}
