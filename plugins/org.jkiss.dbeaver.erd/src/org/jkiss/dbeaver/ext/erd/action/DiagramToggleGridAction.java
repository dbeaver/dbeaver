/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.ext.erd.Activator;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

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