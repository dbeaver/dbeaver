/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.editor.tools;

import org.eclipse.gef3.palette.ToolEntry;
import org.eclipse.gef3.tools.SelectionTool;
import org.jkiss.dbeaver.erd.ui.ERDIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * This class is used to override default GEF icons.
 *
 * @see org.eclipse.gef3.palette.SelectionToolEntry
 */
public class SelectionToolEntry extends ToolEntry {
    public SelectionToolEntry() {
        super("Select", "Select diagram objects", DBeaverIcons.getImageDescriptor(ERDIcon.SELECT), DBeaverIcons.getImageDescriptor(ERDIcon.SELECT), SelectionTool.class);
        setUserModificationPermission(PERMISSION_NO_MODIFICATION);
    }
}
