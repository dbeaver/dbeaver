/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.ext.erd.ERDActivator;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * Action to toggle the layout between manual and automatic
 *
 * @author Serge Rider
 */
public class DiagramToggleGridAction extends Action
{
	public DiagramToggleGridAction()
	{
		super(ERDMessages.erd_editor_control_action_toggle_grid, ERDActivator.getImageDescriptor("icons/layer_grid.png"));
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