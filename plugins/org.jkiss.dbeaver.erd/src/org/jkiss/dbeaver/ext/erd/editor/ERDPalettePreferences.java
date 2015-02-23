/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/*
 * Created on Aug 12, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.jkiss.dbeaver.ext.erd.Activator;


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
		int location = Activator.getDefault().getPreferenceStore().getInt(PALETTE_DOCK_LOCATION);
		if (location == 0)
		{
			return PositionConstants.WEST;
		}
		return location;
	}

	@Override
    public int getPaletteState()
	{
		return Activator.getDefault().getPreferenceStore().getInt(PALETTE_STATE);
	}

	@Override
    public int getPaletteWidth()
	{
		int width = Activator.getDefault().getPreferenceStore().getInt(PALETTE_SIZE);
		if (width == 0)
			return DEFAULT_PALETTE_WIDTH;
		return width;
	}

	@Override
    public void setDockLocation(int location)
	{
		Activator.getDefault().getPreferenceStore().setValue(PALETTE_DOCK_LOCATION, location);
	}

	@Override
    public void setPaletteState(int state)
	{
		Activator.getDefault().getPreferenceStore().setValue(PALETTE_STATE, state);
	}

	@Override
    public void setPaletteWidth(int width)
	{
		Activator.getDefault().getPreferenceStore().setValue(PALETTE_SIZE, width);
	}

}