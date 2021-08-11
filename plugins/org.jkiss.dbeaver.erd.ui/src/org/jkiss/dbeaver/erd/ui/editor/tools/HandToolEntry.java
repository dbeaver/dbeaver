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
package org.jkiss.dbeaver.erd.ui.editor.tools;

import org.eclipse.draw2dl.Cursors;
import org.eclipse.draw2dl.FigureCanvas;
import org.eclipse.draw2dl.geometry.Point;
import org.eclipse.gef3.EditPartViewer;
import org.eclipse.gef3.palette.ToolEntry;
import org.eclipse.gef3.tools.AbstractTool;
import org.eclipse.gef3.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.erd.ui.ERDIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;

public class HandToolEntry extends ToolEntry {
    public static final String ID = "hand-tool";

    public HandToolEntry() {
        super("Pan Diagram", "Pan diagram view", DBeaverIcons.getImageDescriptor(ERDIcon.MOVE), DBeaverIcons.getImageDescriptor(ERDIcon.MOVE), ToolHand.class);
        setUserModificationPermission(PERMISSION_NO_MODIFICATION);
        setId(ID);
    }

    public static class ToolHand extends AbstractTool {
        private int viewStartX;
        private int viewStartY;

        public ToolHand() {
            setDefaultCursor(Cursors.SIZEALL);
            setUnloadWhenFinished(false);
        }

        @Override
        public void deactivate() {
            if (isInState(STATE_DRAG_IN_PROGRESS)) {
                performAbort();
            }
            super.deactivate();
        }

        @Override
        protected boolean handleButtonDown(int button) {
            if (button != 1) {
                setState(STATE_INVALID);
            }
            if (stateTransition(STATE_INITIAL, STATE_DRAG_IN_PROGRESS)) {
                final Point viewLocation = getViewLocation();
                viewStartX = viewLocation.x;
                viewStartY = viewLocation.y;
            }
            return true;
        }

        @Override
        protected boolean handleButtonUp(int button) {
            stateTransition(STATE_DRAG_IN_PROGRESS, STATE_TERMINAL);
            handleFinished();
            return true;
        }

        @Override
        protected boolean handleDragInProgress() {
            if (isInState(STATE_DRAG | STATE_DRAG_IN_PROGRESS)) {
                performMove();
            }
            return true;
        }

        @Override
        protected String getCommandName() {
            return "move";
        }

        private void performMove() {
            final Point delta = getStartLocation().getTranslated(getLocation().getNegated());
            final Point location = getStartViewLocation().getTranslated(delta);
            scrollViewTo(location);
        }

        private void performAbort() {
            scrollViewTo(getStartViewLocation());
        }

        private void scrollViewTo(@NotNull Point point) {
            final FigureCanvas canvas = getCanvas();
            if (canvas != null) {
                canvas.scrollTo(point.x, point.y);
            }
        }

        @NotNull
        private Point getViewLocation() {
            final FigureCanvas canvas = getCanvas();
            if (canvas != null) {
                return canvas.getViewport().getViewLocation().getCopy();
            } else {
                return new Point(0, 0);
            }
        }

        @NotNull
        private Point getStartViewLocation() {
            return new Point(viewStartX, viewStartY);
        }

        @Nullable
        private FigureCanvas getCanvas() {
            final EditPartViewer viewer = getCurrentViewer();
            if (viewer instanceof ScrollingGraphicalViewer) {
                final Control control = viewer.getControl();
                if (control instanceof FigureCanvas) {
                    return (FigureCanvas) control;
                }
            }
            return null;
        }
    }
}
