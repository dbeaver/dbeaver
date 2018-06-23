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

import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.palette.*;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.dbeaver.ext.erd.ERDActivator;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditPartFactory;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

/**
 * ERD object adapter
 */
public class ERDDecoratorDefault implements ERDDecorator {

    public static final ImageDescriptor CONNECT_IMAGE = ERDActivator.getImageDescriptor("icons/connect.png");
    public static final ImageDescriptor NOTE_IMAGE = ERDActivator.getImageDescriptor("icons/note.png");

    @Override
    public boolean showCheckboxes() {
        return false;
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

/*
        {
            //PaletteDrawer controls = new PaletteDrawer("Diagram", ERDActivator.getImageDescriptor("icons/erd.png"));
            PaletteToolbar controls = new PaletteToolbar("Diagram");
            paletteRoot.add(controls);

            controls.add(new ActionToolEntry(new ZoomInAction(rootPart.getZoomManager())));
            controls.add(new ActionToolEntry(new ZoomOutAction(rootPart.getZoomManager())));

            controls.add(new ActionToolEntry(new DiagramLayoutAction(ERDEditorPart.this)));
            controls.add(new ActionToolEntry(new DiagramToggleGridAction()));
            controls.add(new ActionToolEntry(new DiagramRefreshAction(ERDEditorPart.this)));
            controls.add(new PaletteSeparator());
            {
                controls.add(new CommandToolEntry(
                    IWorkbenchCommandConstants.FILE_SAVE_AS,
                    ERDMessages.erd_editor_control_action_save_external_format,
                    UIIcon.PICTURE_SAVE));
                controls.add(new CommandToolEntry(
                    IWorkbenchCommandConstants.FILE_PRINT,
                    ERDMessages.erd_editor_control_action_print_diagram,
                    UIIcon.PRINT));
            }
            {
                Action configAction = new Action(ERDMessages.erd_editor_control_action_configuration) {
                    @Override
                    public void run()
                    {
                        UIUtils.showPreferencesFor(
                            getSite().getShell(),
                            ERDEditorPart.this,
                            ERDPreferencePage.PAGE_ID);
                    }
                };
                configAction.setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
                controls.add(new ActionToolEntry(configAction));
            }
        }
*/

/*
            PaletteDrawer drawer = new PaletteDrawer("New Component",
                ERDActivator.getImageDescriptor("icons/connection.gif"));

            List<CombinedTemplateCreationEntry> entries = new ArrayList<CombinedTemplateCreationEntry>();

            CombinedTemplateCreationEntry tableEntry = new CombinedTemplateCreationEntry("New Table", "Create a new table",
                ERDEntity.class, new DataElementFactory(ERDEntity.class),
                ERDActivator.getImageDescriptor("icons/table.png"),
                ERDActivator.getImageDescriptor("icons/table.png"));

            CombinedTemplateCreationEntry columnEntry = new CombinedTemplateCreationEntry("New Column", "Add a new column",
                ERDEntityAttribute.class, new DataElementFactory(ERDEntityAttribute.class),
                ERDActivator.getImageDescriptor("icons/column.png"),
                ERDActivator.getImageDescriptor("icons/column.png"));

            entries.add(tableEntry);
            entries.add(columnEntry);

            drawer.addAll(entries);

            paletteRoot.add(drawer);
*/
    }

    protected PaletteDrawer createToolsDrawer(PaletteRoot paletteRoot) {
        PaletteDrawer controls = new PaletteDrawer("Tools", DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));

        paletteRoot.add(controls);

        // the selection tool
        ToolEntry selectionTool = new SelectionToolEntry();
        controls.add(selectionTool);

        // use selection tool as default entry
        paletteRoot.setDefaultEntry(selectionTool);
        return controls;
    }

}
