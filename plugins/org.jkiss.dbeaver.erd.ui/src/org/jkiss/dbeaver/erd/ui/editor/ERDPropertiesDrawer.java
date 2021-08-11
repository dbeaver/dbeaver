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
/*
 * Created on Aug 12, 2004
 */
package org.jkiss.dbeaver.erd.ui.editor;

import org.eclipse.gef3.palette.PaletteDrawer;
import org.eclipse.gef3.palette.SelectionToolEntry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

/**
 * Experimental drawer.
 * Unfortunately it is not possible to customize drawer look-and-feel. All UI rendering is performed by .gef3.
 */
public class ERDPropertiesDrawer extends PaletteDrawer
{

	public ERDPropertiesDrawer() {
		super("Properties", DBeaverIcons.getImageDescriptor(UIIcon.PROPERTIES));
		setDescription("Object properties");
		setId("erd-properties");

		add(new SelectionToolEntry());
	}

}