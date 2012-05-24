/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class SampleAction extends Action implements IViewActionDelegate {

	public SampleAction() {
	}

	@Override
    public void init(IViewPart view) {
	}

	@Override
    public void run(IAction action) {
	}

	@Override
    public void selectionChanged(IAction action, ISelection selection) {
	}
}
