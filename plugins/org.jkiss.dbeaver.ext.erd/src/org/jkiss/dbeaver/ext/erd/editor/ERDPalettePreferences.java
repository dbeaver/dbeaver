/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/*
 * Created on Aug 12, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.jkiss.dbeaver.ext.erd.ERDActivator;


/**
 * Contains the preferences for the palette flyout
 * @author Serge Rieder
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
		int location = ERDActivator.getDefault().getPreferenceStore().getInt(PALETTE_DOCK_LOCATION);
		if (location == 0)
		{
			return PositionConstants.WEST;
		}
		return location;
	}

	@Override
    public int getPaletteState()
	{
		return ERDActivator.getDefault().getPreferenceStore().getInt(PALETTE_STATE);
	}

	@Override
    public int getPaletteWidth()
	{
		int width = ERDActivator.getDefault().getPreferenceStore().getInt(PALETTE_SIZE);
		if (width == 0)
			return DEFAULT_PALETTE_WIDTH;
		return width;
	}

	@Override
    public void setDockLocation(int location)
	{
		ERDActivator.getDefault().getPreferenceStore().setValue(PALETTE_DOCK_LOCATION, location);
	}

	@Override
    public void setPaletteState(int state)
	{
		ERDActivator.getDefault().getPreferenceStore().setValue(PALETTE_STATE, state);
	}

	@Override
    public void setPaletteWidth(int width)
	{
		ERDActivator.getDefault().getPreferenceStore().setValue(PALETTE_SIZE, width);
	}

}