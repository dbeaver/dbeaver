/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.widgets.Event;
import org.jkiss.dbeaver.ui.DefaultViewerToolTipSupport;

class DatabaseNavigatorToolTipSupport extends DefaultViewerToolTipSupport {
    private DatabaseNavigatorTree databaseNavigatorTree;

    DatabaseNavigatorToolTipSupport(DatabaseNavigatorTree databaseNavigatorTree) {
        super(databaseNavigatorTree.getViewer());
        this.databaseNavigatorTree = databaseNavigatorTree;
        // Reset tooltip cache otherwise old tooltip blinks before new one
        databaseNavigatorTree.getViewer().getControl().addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseExit(MouseEvent e) {
                databaseNavigatorTree.getViewer().getControl().setToolTipText(null);
            }
        });
    }

    @Override
    protected boolean shouldCreateToolTip(Event event) {
        return super.shouldCreateToolTip(event);
    }
}
