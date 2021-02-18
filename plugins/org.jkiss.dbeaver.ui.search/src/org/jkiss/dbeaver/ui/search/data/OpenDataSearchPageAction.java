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
package org.jkiss.dbeaver.ui.search.data;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Opens the data search dialog.
 */
public class OpenDataSearchPageAction implements IWorkbenchWindowActionDelegate {

	private static final String DATA_SEARCH_PAGE_ID = SearchDataPage.class.getName();

	private IWorkbenchWindow fWindow;

	public OpenDataSearchPageAction() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
		fWindow= window;
	}

	@Override
	public void run(IAction action) {
		if (fWindow == null || fWindow.getActivePage() == null) {
			return;
		}
		NewSearchUI.openSearchDialog(fWindow, DATA_SEARCH_PAGE_ID);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing since the action isn't selection dependent.
	}

	@Override
	public void dispose() {
		fWindow= null;
	}

}
