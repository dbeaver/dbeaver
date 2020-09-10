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
/*
 * Created on Aug 12, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.jkiss.dbeaver.ext.erd.ERDUIActivator;


/**
 * Contains the preferences for the palette flyout
 * @author Serge Rider
 */
public class ERDPalettePreferences implements FlyoutPreferences
{

	public static final int DEFAULT_PALETTE_WIDTH = 150;

	protected static final String PALETTE_DOCK_LOCATION = "Dock location";
	protected static final String PALETTE_SIZE = "Palette Size";
	protected static final String PALETTE_STATE = "Palette state";

	@Override
    public int getDockLocation()
	{
		int location = ERDUIActivator.getDefault().getPreferenceStore().getInt(PALETTE_DOCK_LOCATION);
		if (location == 0)
		{
			return PositionConstants.EAST;
		}
		return location;
	}

	@Override
    public int getPaletteState()
	{
		return ERDUIActivator.getDefault().getPreferenceStore().getInt(PALETTE_STATE);
	}

	@Override
    public int getPaletteWidth()
	{
		int width = ERDUIActivator.getDefault().getPreferenceStore().getInt(PALETTE_SIZE);
		if (width == 0)
			return DEFAULT_PALETTE_WIDTH;
		return width;
	}

	@Override
    public void setDockLocation(int location)
	{
		ERDUIActivator.getDefault().getPreferenceStore().setValue(PALETTE_DOCK_LOCATION, location);
	}

	@Override
    public void setPaletteState(int state)
	{
		ERDUIActivator.getDefault().getPreferenceStore().setValue(PALETTE_STATE, state);
	}

	@Override
    public void setPaletteWidth(int width)
	{
		ERDUIActivator.getDefault().getPreferenceStore().setValue(PALETTE_SIZE, width);
	}

}