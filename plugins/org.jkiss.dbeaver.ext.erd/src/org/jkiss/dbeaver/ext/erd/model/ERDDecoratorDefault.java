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
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.palette.*;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.ERDActivator;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.editor.ERDAttributeVisibility;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditPartFactory;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * ERD object adapter
 */
public class ERDDecoratorDefault implements ERDDecorator {

    public static final ImageDescriptor CONNECT_IMAGE = ERDActivator.getImageDescriptor("icons/connect.png");
    public static final ImageDescriptor NOTE_IMAGE = ERDActivator.getImageDescriptor("icons/note.png");

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
    public Insets getDefaultEntityInsets() {
        return new Insets(20, 20, 10, 20);
    }

    @Override
    public EditPartFactory createPartFactory() {
        return new ERDEditPartFactory();
    }

    @Override
    public void fillPalette(PaletteRoot paletteRoot, boolean readOnly) {
        {
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

                controls.add(new ConnectionCreationToolEntry(ERDMessages.erd_tool_create_connection, ERDMessages.erd_tool_create_connection_tip, null, CONNECT_IMAGE, CONNECT_IMAGE));
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
    }

    protected PaletteDrawer createToolsDrawer(PaletteRoot paletteRoot) {
        PaletteDrawer controls = new PaletteDrawer("Tools", DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));

        paletteRoot.add(controls);

        return controls;
    }

    @Override
    public void fillEntityFromObject(DBRProgressMonitor monitor, EntityDiagram diagram, ERDEntity erdEntity) {
        DBSEntity entity = erdEntity.getObject();
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
                idColumns = ERDUtils.getBestTableIdentifier(monitor, entity);
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
                        if (DBUtils.isHiddenObject(attribute) || DBUtils.isInheritedObject(attribute)) {
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

    @Override
    public ERDAssociation createAutoAssociation(ERDContainer diagram, DBSEntityAssociation association, ERDEntity sourceEntity, ERDEntity targetEntity, boolean reflect) {
        // Allow all auto-associations
        return new ERDAssociation(association, sourceEntity, targetEntity, reflect);
    }

}
