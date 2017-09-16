/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mariot Chauvin <mariot.chauvin@obeo.fr> - bug 259553
 *     Amit Joglekar <joglekar@us.ibm.com> - Support for dynamic images (bug 385795)
 *
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

package org.jkiss.dbeaver.ui.controls.folders;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;


/**
 * Persisted state of tabbed folders
 */
public class TabbedFolderState {

    static class TabState {
        int height;
        int width;
        boolean embedded;
    }

    private Map<String, TabState> tabStates = new HashMap<>();

    public TabState getTabState(String tabId) {
        return tabStates.get(tabId);
    }

    Map<String, TabState> getTabStates() {
        return tabStates;
    }

    TabState getTabState(String tabId, boolean create) {
        TabState state = tabStates.get(tabId);
        if (state == null && create) {
            state = new TabState();
            tabStates.put(tabId, state);
        }
        return state;
    }

    public void setTabState(String tabId, TabState state) {
        tabStates.put(tabId, state);
    }

    public void setTabHeight(String tabId, int height) {
        getTabState(tabId, true).height = height;
    }

    public void setTabWidth(String tabId, int width) {
        getTabState(tabId, true).width = width;
    }

    public void setTabEmbedded(String tabId, boolean embedded) {
        getTabState(tabId, true).embedded = embedded;
    }
}
