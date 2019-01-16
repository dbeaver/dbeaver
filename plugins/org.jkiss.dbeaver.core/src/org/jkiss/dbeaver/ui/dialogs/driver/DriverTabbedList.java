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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.controls.finder.viewer.AdvancedListViewer;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolder;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderComposite;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderInfo;

/**
 * DriverTabbedList
 */
public class DriverTabbedList extends Composite {
    private static final Log log = Log.getLog(DriverTabbedList.class);

    public DriverTabbedList(Composite parent, int style) {
        super(parent, style);

        TabbedFolderComposite folderComposite = new TabbedFolderComposite(this, SWT.NONE);

        TabbedFolderInfo[] folders = new TabbedFolderInfo[] {

        };
        folderComposite.setFolders(getClass().getSimpleName(), folders);
    }

    private static class DriverListFolder implements ITabbedFolder {

        private AdvancedListViewer viewer;
        @Override
        public void createControl(Composite parent) {
            viewer = new AdvancedListViewer(parent, SWT.NONE);
        }

        @Override
        public void aboutToBeShown() {

        }

        @Override
        public void aboutToBeHidden() {

        }

        @Override
        public void setFocus() {
            viewer.getControl().setFocus();
        }

        @Override
        public void dispose() {
        }
    }

}
