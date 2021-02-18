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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.widgets.Event;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;

/**
 * Tooltips
 */
public class DefaultViewerToolTipSupport extends ColumnViewerToolTipSupport {

    public DefaultViewerToolTipSupport(ColumnViewer viewer) {
        super(viewer, ToolTip.NO_RECREATE, false);
    }

    @Override
    protected void afterHideToolTip(Event event) {
        if (AbstractPopupPanel.isPopupOpen()) {
            // Do not pass event to avoid focus change (#7908)
            super.afterHideToolTip(null);
        } else {
            super.afterHideToolTip(event);
        }
    }
}
