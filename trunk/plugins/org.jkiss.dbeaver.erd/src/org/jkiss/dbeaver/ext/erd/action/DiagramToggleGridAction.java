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
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.ext.erd.Activator;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

/**
 * Action to toggle the layout between manual and automatic
 *
 * @author Serge Rieder
 */
public class DiagramToggleGridAction extends Action
{
	public DiagramToggleGridAction()
	{
		super("Toggle Grid", Activator.getImageDescriptor("icons/layer_grid.png"));
	}

	@Override
    public void run()
	{
        final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        final boolean gridEnabled = store.getBoolean(ERDConstants.PREF_GRID_ENABLED);
        store.setValue(ERDConstants.PREF_GRID_ENABLED, !gridEnabled);
        RuntimeUtils.savePreferenceStore(store);
    }

}