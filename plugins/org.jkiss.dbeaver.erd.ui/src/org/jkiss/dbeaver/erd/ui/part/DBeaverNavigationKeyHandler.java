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
package org.jkiss.dbeaver.erd.ui.part;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

public class DBeaverNavigationKeyHandler extends GraphicalViewerKeyHandler {
    public DBeaverNavigationKeyHandler(GraphicalViewer viewer) {
        super(viewer);
    }

    boolean additionalAcceptIntoContainer(KeyEvent event) {
        return event.keyCode == SWT.CR;
    }

    boolean additionalOutOfContainer(KeyEvent event) {
        // Backspace
        return event.keyCode == 8;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (additionalAcceptIntoContainer(event)) {
            if (getFocusEditPart() instanceof NotePart) {
                ((NotePart) getFocusEditPart()).performDirectEdit();
                return true;
            }
            event.stateMask = SWT.ALT;
            event.keyCode = SWT.ARROW_DOWN;
        }
        if (additionalOutOfContainer(event)) {
            event.stateMask = SWT.ALT;
            event.keyCode = SWT.ARROW_UP;
        }
        return super.keyPressed(event);
    }
}
