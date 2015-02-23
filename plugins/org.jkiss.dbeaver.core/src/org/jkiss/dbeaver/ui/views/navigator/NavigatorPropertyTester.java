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
package org.jkiss.dbeaver.ui.views.navigator;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ui.controls.folders.IFolderContainer;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.ui.editors.MultiPageAbstractEditor;

/**
 * DatabaseEditorPropertyTester
 */
public class NavigatorPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.core.navigator";
    public static final String PROP_ACTIVE = "active";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        INavigatorModelView nmv = getActiveNavigator((IWorkbenchPart)receiver);
        return nmv != null && checkNavigatorProperty(nmv, property, expectedValue);
    }

    private boolean checkNavigatorProperty(INavigatorModelView rsv, String property, Object expectedValue)
    {
        if (PROP_ACTIVE.equals(property)) {
            return true;
        }
        return false;
    }

    public static INavigatorModelView getActiveNavigator(IWorkbenchPart activePart) {
        //IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart instanceof INavigatorModelView) {
            return ((INavigatorModelView) activePart);
        } else if (activePart instanceof MultiPageAbstractEditor) {
            return getActiveNavigator(((MultiPageAbstractEditor) activePart).getActiveEditor());
        } else if (activePart instanceof IFolderContainer) {
            Object activeFolder = ((IFolderContainer) activePart).getActiveFolder();
            if (activeFolder instanceof INavigatorModelView) {
                return (INavigatorModelView)activeFolder;
            } else if (activeFolder instanceof IWorkbenchPart) {
                return getActiveNavigator((IWorkbenchPart) activeFolder);
            }
        }
        return null;
    }
}