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
package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolderContainer;
import org.jkiss.dbeaver.ui.editors.MultiPageAbstractEditor;

/**
 * DatabaseEditorPropertyTester
 */
public class NavigatorPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.core.navigator";
    public static final String PROP_ACTIVE = "active";
    public static final String PROP_FOCUSED = "focused";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        INavigatorModelView nmv = getActiveNavigator((IWorkbenchPart)receiver);
        return nmv != null && checkNavigatorProperty(nmv, property, expectedValue);
    }

    private boolean checkNavigatorProperty(INavigatorModelView rsv, String property, Object expectedValue)
    {
        switch (property) {
            case PROP_ACTIVE:
                return true;
            case PROP_FOCUSED:
                Viewer viewer = rsv.getNavigatorViewer();
                return viewer != null && viewer.getControl() != null &&
                        !viewer.getControl().isDisposed() && viewer.getControl().isFocusControl();
        }
        return false;
    }

    public static INavigatorModelView getActiveNavigator(IWorkbenchPart activePart) {
        //IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart == null) {
            return null;
        } else if (activePart instanceof INavigatorModelView) {
            return ((INavigatorModelView) activePart);
        } else if (activePart instanceof MultiPageAbstractEditor) {
            return getActiveNavigator(((MultiPageAbstractEditor) activePart).getActiveEditor());
        } else if (activePart instanceof ITabbedFolderContainer) {
            Object activeFolder = ((ITabbedFolderContainer) activePart).getActiveFolder();
            if (activeFolder instanceof INavigatorModelView) {
                return (INavigatorModelView)activeFolder;
            } else if (activeFolder instanceof IWorkbenchPart) {
                return getActiveNavigator((IWorkbenchPart) activeFolder);
            }
        }
        return activePart.getAdapter(INavigatorModelView.class);
    }

}