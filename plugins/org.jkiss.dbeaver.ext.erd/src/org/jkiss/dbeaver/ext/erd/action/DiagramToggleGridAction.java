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
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.ext.erd.ERDActivator;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * Action to toggle the layout between manual and automatic
 *
 * @author Serge Rieder
 */
public class DiagramToggleGridAction extends Action
{
	public DiagramToggleGridAction()
	{
		super("Toggle Grid", ERDActivator.getImageDescriptor("icons/layer_grid.png"));
	}

	@Override
    public void run()
	{
        final DBPPreferenceStore store = ERDActivator.getDefault().getPreferences();
        final boolean gridEnabled = store.getBoolean(ERDConstants.PREF_GRID_ENABLED);
        store.setValue(ERDConstants.PREF_GRID_ENABLED, !gridEnabled);
        PrefUtils.savePreferenceStore(store);
    }

}