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
package org.jkiss.dbeaver.erd.ui.model;

import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.palette.*;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDNote;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditPartFactory;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

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
                ERDUIMessages.erd_tool_create_connection,
                ERDUIMessages.erd_tool_create_connection_tip,
                null,
                CONNECT_IMAGE,
                CONNECT_IMAGE));
            controls.add(new CreationToolEntry(
                ERDUIMessages.erd_tool_create_note,
                ERDUIMessages.erd_tool_create_note_tip,
                new CreationFactory() {
                    @Override
                    public Object getNewObject()
                    {
                        return new ERDNote(ERDUIMessages.erd_tool_create_default);
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

}
