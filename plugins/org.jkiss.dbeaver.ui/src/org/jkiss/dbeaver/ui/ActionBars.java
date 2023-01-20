/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.*;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.part.EditorActionBarContributor;

public class ActionBars {

    public static IStatusLineManager extractStatusLineManager(IWorkbenchSite site) {
        IActionBars actionBars = extractActionBars(site);
        if (actionBars == null) {
            return null;
        }
        return actionBars.getStatusLineManager();
    }

    public static IActionBars extractActionBars(IWorkbenchSite site) {
        if (site == null) {
            return null;
        }
        IWorkbenchPage page= site.getPage();
        IWorkbenchPart activePart= page.getActivePart();

        if (activePart instanceof IViewPart) {
            IViewPart activeViewPart= (IViewPart)activePart;
            IViewSite activeViewSite= activeViewPart.getViewSite();
            return activeViewSite.getActionBars();
        }

        if (activePart instanceof IEditorPart) {
            IEditorPart activeEditorPart= (IEditorPart)activePart;
            IEditorActionBarContributor contributor= activeEditorPart.getEditorSite().getActionBarContributor();
            if (contributor instanceof EditorActionBarContributor) {
                return ((EditorActionBarContributor) contributor).getActionBars();
            }
        }
        if (site instanceof IViewSite) {
            IViewSite viewSite = (IViewSite) site;
            return viewSite.getActionBars();
        }
        if (site instanceof IEditorSite) {
            IEditorSite editorSite = (IEditorSite) site;
            return editorSite.getActionBars();
        }
        if (site instanceof IIntroSite) {
            IIntroSite introSite = (IIntroSite) site;
            return introSite.getActionBars();
        }
        //OMG, what is it?
        return null;
    }
}
