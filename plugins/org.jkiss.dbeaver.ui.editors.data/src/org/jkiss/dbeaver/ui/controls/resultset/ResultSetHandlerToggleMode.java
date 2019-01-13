/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import java.util.Map;

/**
 * ResultSetHandlerMain
 */
public class ResultSetHandlerToggleMode extends ResultSetHandlerMain implements IElementUpdater {

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (element.getServiceLocator() instanceof IWorkbenchPartSite) {
            IWorkbenchPartSite partSite = (IWorkbenchPartSite) element.getServiceLocator();
            if (partSite.getPart() instanceof IResultSetContainer) {
                IResultSetController rsv = ((IResultSetContainer) partSite.getPart()).getResultSetController();
                if (rsv != null) {
                    if (!rsv.isRecordMode()) {
                        element.setText("Switch to record mode");
                        element.setChecked(true);
                    } else {
                        element.setText("Switch to grid mode");
                        element.setChecked(false);
                    }
                }
            }
        }
    }
}