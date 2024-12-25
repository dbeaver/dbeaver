/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sybase, Inc. - extended for DTP
 *******************************************************************************/
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls;
/*
 *  CustomSashForm
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.internal.UIMessages;

import java.util.ArrayList;
import java.util.List;

/**
 * A SashForm that allows hide/restore controls on sash.
 * <p>
 * It only works with one sash (two children). It doesn't make sense
 * for the arrows when there is more than one sash. Things get confusing for
 * a restore position.
 */
public class CustomSashForm extends SashForm {

    private static final Log log = Log.getLog(CustomSashForm.class);

    /**
     * Custom style bits. They set whether hide to one side of the other
     * is not permitted. For example, if NO_HIDE_UP, then there will be only
     * one arrow. When not hidden, it will point down (and will do a hide down),
     * and when hidden down, it will point up (and will do a restore to the
     * previous weight). There won't be a hide to the top arrow.
     */
    public static final int
            NO_HIDE_LEFT = 0x1,            // Custom style bit for not allow hide left
            NO_HIDE_UP = NO_HIDE_LEFT,    // Custom style bit for not allow hide up
            NO_HIDE_RIGHT = 0x2,            // Custom style bit for not allow hide right
            NO_HIDE_DOWN = NO_HIDE_RIGHT;    // Custom style bit for not allow hide down


    private static final int NO_WEIGHT = -1;
    private static final int NO_ARROW = -1;
    
    protected Color arrowColor;

    private static class SashInfo {
        public Sash sash;
        public boolean enabled;    // Whether this sashinfo is enabled (i.e. if there is more than one, this will be disabled).
        public int restoreWeight = NO_WEIGHT;    // If slammed to an edge this is the restore weight. -1 means not slammed. This is the restoreWeight in the 2nd section form, i.e. weights[1].
        public int cursorOver = NO_ARROW;    // Which arrow is cursor over,
        public boolean sashBorderLeft;    // Draw sash border left/top
        public boolean sashBorderRight;    // Draw sash border right/bottom
        public int[][] sashLocs;    // There is one entry for each arrow, It is arrowType/arrowDrawn/x/y/height/width of the arrow area.
        // There may not be a second entry, in which case we have only one arrow.
        public Point[] savedSizes = new Point[2];  // Saved sizes of controls - saved whenever a control is hidden or restored

        public SashInfo(Sash sash) {
            this.sash = sash;
        }
    }

    public static final Color DEFAULT_BORDER_COLOR = new Color(153, 180, 209, 255);


    public interface ICustomSashFormListener {
        void dividerMoved(int firstControlWeight, int secondControlWeight);
    }

    protected SashInfo currentSashInfo = null;    // When the sash goes away, its entry is made null.
    protected boolean inMouseClick = false;    // Because we can't stop drag even when we are in the arrow area, we need
    // to know that mouse down is in process so that when drag is completed, we
    // know not to recompute our position because a mouse up is about to happen
    // and we want the correct arrow handled correctly.

    private boolean[] sashBorders;    // Whether corresponding control needs a sash border
    private boolean showBorders = false;

    protected boolean noHideUp, noHideDown;
    protected List<ICustomSashFormListener> customSashFormListeners = null;

    protected static final int
            UP_RESTORE_ARROW = 0,
            UP_HIDE_ARROW = 1,
            DOWN_RESTORE_ARROW = 2,
            DOWN_HIDE_ARROW = 3,
            HIDE_ARROWS = 4;

    protected static final int
            ARROW_TYPE_INDEX = 0,
            ARROW_DRAWN_INDEX = 1,
            X_INDEX = 2,
            Y_INDEX = 3,
            WIDTH_INDEX = 4,
            HEIGHT_INDEX = 5;

    // These are for the up/down arrow. Just swap them for left/right arrow.
    protected static final int
            ARROW_WIDTH = 12,
            ARROW_HEIGHT = 5,
            ARROW_MARGIN = 5;    // Margin on each side of arrow

    public CustomSashForm(Composite parent, int style) {
        super(parent, style);

        this.addListener(SWT.Resize, new Listener() {
            public void handleEvent(Event e) {
                layout(true);
                removeListener(SWT.Resize, this);
            }
        });

        sashBorders = new boolean[]{true, true};

        SASH_WIDTH = ARROW_HEIGHT + ARROW_MARGIN;

        arrowColor = DEFAULT_BORDER_COLOR;
    }

    public boolean isShowBorders() {
        return showBorders;
    }

    public void setShowBorders(boolean showBorders) {
        this.showBorders = showBorders;
    }

    /**
     * Sets the <code>noHideUp</code> setting for vertical CustomSashForm.
     */
    public void setNoHideUp(boolean bHide) {
        noHideUp = bHide;
    }

    /**
     * Sets the <code>noHideDown</code> setting for vertical CustomSashForm.
     */
    public void setNoHideDown(boolean bHide) {
        noHideDown = bHide;
    }

    /**
     * Sets the <code>noHideLeft</code> setting for horizontal CustomSashForm.
     */
    public void setNoHideLeft(boolean bHide) {
        setNoHideUp(bHide);
    }

    /**
     * Sets the <code>noHideRight</code> setting for horizontal CustomSashForm.
     */
    public void setNoHideRight(boolean bHide) {
        setNoHideDown(bHide);
    }

    /**
     * Call to set to hide up
     */
    public void hideUp() {
        if (noHideUp)
            return;

        if (currentSashInfo == null)
            currentSashInfo = new SashInfo(null);
        upHideClicked(currentSashInfo);
    }

    /**
     * Call to set to hide left
     */
    public void hideLeft() {
        hideUp();
    }


    /**
     * Call to set to hide down
     */
    public void hideDown() {
        if (noHideDown)
            return;

        if (currentSashInfo == null)
            currentSashInfo = new SashInfo(null);

        downHideClicked();
    }

    public boolean isDownHidden() {
        if (currentSashInfo == null || currentSashInfo.restoreWeight <= 0) {
            return false;
        }
        int[] weights = getWeights();
        return weights.length == 2 && weights[1] == 0;
    }

    public void showDown() {
        if (currentSashInfo == null || currentSashInfo.restoreWeight <= 0) {
            hideDown();
        }
        downRestoreClicked(currentSashInfo);
    }

    /**
     * Call to set to hide right
     */
    public void hideRight() {
        hideDown();
    }

    /**
     * Set the need sash borders for the controls.
     */
    public void setSashBorders(boolean[] sashBorders) {
        int[] weights = getWeights();    // KLUDGE This is a kludge just to see how many controls we have.
        if (weights.length != 2 || (sashBorders != null && sashBorders.length != 2)) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }
        this.sashBorders = sashBorders;
    }

    /**
     * @see org.eclipse.swt.widgets.Composite#layout(boolean)
     */
    public void layout(boolean changed) {
        super.layout(changed);

        if (noHideUp && noHideDown)
            return;    // No arrows to handle in this case.

        if (getMaximizedControl() != null)
            return;    // We have a maximized control, so we don't need to worry about the sash.

        // Let's get the list of all sashes the sash form now has. If there is more than one then just disable the sashinfo.
        // If there is no current sash, and there is only one sash, then create the sashinfo for it.
        Control[] children = getChildren();
        Sash newSash = null;
        for (Control child : children) {
            if (child instanceof Sash) {
                if (newSash == null) {
                    newSash = (Sash) child;
                } else {
                    // We have more than one sash, so need to disable current sash, if we have one.
                    if (currentSashInfo != null)
                        currentSashInfo.enabled = false;
                    return;    // Don't go on.
                }
            }
        }

        if (newSash == null)
            return;    // We have no sashes at all.

        // Now we need to see if this is a new sash.
        if (currentSashInfo == null || currentSashInfo.sash == null) {
            if (currentSashInfo == null)
                currentSashInfo = new SashInfo(newSash);
            else
                currentSashInfo.sash = newSash;
            newSash.addPaintListener(e -> {
                // Need to find the index of the sash we're interested in.

                GC gc = e.gc;
                final Color oldFg = gc.getForeground();
                final Color oldBg = gc.getBackground();

                boolean isTwoArrows = currentSashInfo.sashLocs.length > 1;
                drawArrow(gc, currentSashInfo.sashLocs[0], currentSashInfo.cursorOver == 0, isTwoArrows);    // Draw first arrow
                if (isTwoArrows) {
                    drawArrow(gc, currentSashInfo.sashLocs[1], currentSashInfo.cursorOver == 1, isTwoArrows);    // Draw second arrow
                }

                {
                    drawSashBorder(gc, currentSashInfo);
                    //if (currentSashInfo.sashBorderLeft)
                    //if (currentSashInfo.sashBorderRight)
                    //    drawSashBorder(gc, currentSashInfo.sash, false);
                }

                gc.setForeground(oldFg);
                gc.setBackground(oldBg);
            });

            newSash.addControlListener(new ControlListener() {
                public void controlMoved(ControlEvent e) {
                    recomputeSashInfo();
                    if (currentSashInfo != null) {
                        currentSashInfo.sash.redraw();
                    }
                }
                public void controlResized(ControlEvent e) {
                    recomputeSashInfo();
                    currentSashInfo.sash.redraw();
                }


            });

            newSash.addDisposeListener(e -> {
                // Need to clear out the widget from current.
                currentSashInfo = null;
            });


            // This is a kludge because we can't override the set cursor hit test.
            newSash.addMouseMoveListener(e -> {
                // See if within one of the arrows.
                int x = e.x;
                int y = e.y;
                for (int i = 0; i < currentSashInfo.sashLocs.length; i++) {
                    int[] locs = currentSashInfo.sashLocs[i];
                    boolean vertical = getOrientation() == SWT.VERTICAL;
                    int loc = vertical ? x : y;
                    int locIndex = vertical ? X_INDEX : Y_INDEX;
                    int sizeIndex = vertical ? WIDTH_INDEX : HEIGHT_INDEX;
                    // Does the mouse position lie within the bounds of the arrow?
                    if (locs[locIndex] <= loc && loc <= locs[locIndex] + locs[sizeIndex]) {
                        if (currentSashInfo.cursorOver == NO_ARROW) {
                            currentSashInfo.sash.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                        }
                        if (currentSashInfo.cursorOver != i) {
                            currentSashInfo.cursorOver = i;
                            //currentSashInfo.sash.redraw();
                            switch (locs[ARROW_TYPE_INDEX]) {
                                case UP_RESTORE_ARROW:
                                case DOWN_RESTORE_ARROW:
                                    currentSashInfo.sash.setToolTipText(UIMessages.tooltip_restore);
                                    break;
                                case UP_HIDE_ARROW:
                                case DOWN_HIDE_ARROW:
                                    currentSashInfo.sash.setToolTipText(UIMessages.tooltip_hide);
                                    break;
                                default:
                                    break;
                            }
                        }
                        return;
                    }
                }
                // If we got here, the mouse position does not lie within the bounds of an arrow
                if (currentSashInfo.cursorOver != NO_ARROW) {
                    currentSashInfo.sash.setCursor(null);
                    currentSashInfo.cursorOver = NO_ARROW;
                    //currentSashInfo.sash.redraw();
                    currentSashInfo.sash.setToolTipText(null);
                }
            });

            // Need to know when we leave so that we can clear the cursor feedback if set.
            newSash.addMouseTrackListener(new MouseTrackAdapter() {
                /**
                 * @see org.eclipse.swt.events.MouseTrackAdapter#mouseExit(MouseEvent)
                 */
                public void mouseExit(MouseEvent e) {
                    if (currentSashInfo.cursorOver != NO_ARROW) {
                        // Undo the cursor.
                        currentSashInfo.sash.setCursor(null);
                        currentSashInfo.cursorOver = NO_ARROW;
                        //currentSashInfo.sash.redraw();
                        currentSashInfo.sash.setToolTipText(null);
                    }
                }
            });

            // Want to handle mouse down as a selection.
            newSash.addMouseListener(new MouseAdapter() {
                public void mouseDown(MouseEvent e) {
                    inMouseClick = true;
                    // If we're within a button, then redraw to wipe out stipple and get button push effect.
                    int x = e.x;
                    int y = e.y;
                    for (int i = 0; i < currentSashInfo.sashLocs.length; i++) {
                        int[] locs = currentSashInfo.sashLocs[i];
                        boolean vertical = getOrientation() == SWT.VERTICAL;
                        int loc = vertical ? x : y;
                        int locIndex = vertical ? X_INDEX : Y_INDEX;
                        int sizeIndex = vertical ? WIDTH_INDEX : HEIGHT_INDEX;
                        if (locs[locIndex] <= loc && loc <= locs[locIndex] + locs[sizeIndex]) {
                            currentSashInfo.sash.redraw();
                            break;
                        }
                    }
                }

                public void mouseUp(MouseEvent e) {
                    // See if within one of the arrows.
                    inMouseClick = false;    // No longer in down click
                    int x = e.x;
                    int y = e.y;
                    for (int i = 0; i < currentSashInfo.sashLocs.length; i++) {
                        int[] locs = currentSashInfo.sashLocs[i];
                        boolean vertical = getOrientation() == SWT.VERTICAL;
                        int loc = vertical ? x : y;
                        int locIndex = vertical ? X_INDEX : Y_INDEX;
                        int sizeIndex = vertical ? WIDTH_INDEX : HEIGHT_INDEX;
                        // Does the mouse position lie within the bounds of the arrow?
                        if (locs[locIndex] <= loc && loc <= locs[locIndex] + locs[sizeIndex]) {
                            // We found it.
                            switch (locs[ARROW_TYPE_INDEX]) {
                                case UP_RESTORE_ARROW:
                                    upRestoreClicked(currentSashInfo);
                                    break;
                                case UP_HIDE_ARROW:
                                    upHideClicked(currentSashInfo);
                                    break;
                                case DOWN_RESTORE_ARROW:
                                    downRestoreClicked(currentSashInfo);
                                    break;
                                case DOWN_HIDE_ARROW:
                                    downHideClicked();
                                    break;
                            }
                            break;
                        }
                    }

                    currentSashInfo.sash.redraw();    // Make sure stipple goes away from the mouse up if not over an arrow button.
                    fireDividerMoved();
                }
            });

            recomputeSashInfo();    // Get initial setting
        }
    }

    /*
     * Constants for recording whether the sash is slammed to the top/bottom or not slammed
     */
    private final static int NOT_SLAMMED = 1;
    private final static int SLAMMED_TO_BOTTOM = 2;
    private final static int SLAMMED_TO_TOP = 3;

    protected void recomputeSashInfo() {
        if (inMouseClick && currentSashInfo.cursorOver != NO_ARROW) {
            return;    // Don't process because we are in the down mouse button on an arrow.
        }

        // addArrows are the types of the arrows - hide/restore/up/down and
        // drawArrows are the types of the arrows actually drawn. Here
        // drawArrows are always RESTORE arrow types, so that the UI only
        // has a single arrow type showing.
        int[] addArrows;
        int[] drawArrows;

        // We need to refigure size for the sash arrows.
        int[] weights = getWeights();    // This should be two entries only. We shouldn't have got here if there were more than two.

        // TODO: Use of DRAG_MINIMUM is a kludge, required because SashForm only allows you to close each part so far
        final int DRAG_MINIMUM = 20; // TODO: kludge see SashForm.DRAG_MINIMUM
        Rectangle sashBounds = currentSashInfo.sash.getBounds();
        Rectangle clientArea = getClientArea();
        boolean vertical = getOrientation() == SWT.VERTICAL;

        //
        // Work out whether the sash is slammed to the top / bottom or not slammed
        //
        int slammed = NOT_SLAMMED;
        if (weights[1] == 0) {
            slammed = SLAMMED_TO_BOTTOM;
        } else if (weights[0] == 0) {
            slammed = SLAMMED_TO_TOP;
        } else if (vertical) {
            if (currentSashInfo.restoreWeight != NO_WEIGHT && sashBounds.y <= DRAG_MINIMUM) {
                slammed = SLAMMED_TO_TOP;
            } else if (currentSashInfo.restoreWeight != NO_WEIGHT && sashBounds.y + sashBounds.height >= clientArea.height - DRAG_MINIMUM) {
                slammed = SLAMMED_TO_BOTTOM;
            }
        } else {
            if (currentSashInfo.restoreWeight != NO_WEIGHT && sashBounds.x <= DRAG_MINIMUM) {
                slammed = SLAMMED_TO_TOP;
            } else if (currentSashInfo.restoreWeight != NO_WEIGHT && sashBounds.x + sashBounds.width >= clientArea.width - DRAG_MINIMUM) {
                slammed = SLAMMED_TO_BOTTOM;
            }
        }

        //
        // Now decide which arrows to add, according to whether noHideUp, noHideDown and the slammed status.
        //
        if (noHideUp) {
            if (slammed == SLAMMED_TO_BOTTOM) {
                addArrows = new int[1];
                drawArrows = new int[1];

                addArrows[0] = UP_RESTORE_ARROW;
                drawArrows[0] = UP_RESTORE_ARROW;
                currentSashInfo.sashBorderLeft = sashBorders != null && sashBorders[0];
                currentSashInfo.sashBorderRight = false;
            } else {
                //Not slammed
                addArrows = new int[1];
                drawArrows = new int[1];

                addArrows[0] = DOWN_HIDE_ARROW;
                drawArrows[0] = DOWN_RESTORE_ARROW;
                currentSashInfo.restoreWeight = NO_WEIGHT;    // Since we are in the middle, there is no restoreWeight. We've could of been dragged here.
                currentSashInfo.sashBorderLeft = sashBorders != null && sashBorders[0];
                currentSashInfo.sashBorderRight = sashBorders != null && sashBorders[1];
            }
        } else if (noHideDown) {
            if (slammed == SLAMMED_TO_TOP) {
                addArrows = new int[1];
                drawArrows = new int[1];

                addArrows[0] = DOWN_RESTORE_ARROW;
                drawArrows[0] = DOWN_RESTORE_ARROW;
                currentSashInfo.sashBorderLeft = false;
                currentSashInfo.sashBorderRight = sashBorders != null && sashBorders[1];
            } else {
                //Not slammed
                addArrows = new int[1];
                drawArrows = new int[1];

                addArrows[0] = UP_HIDE_ARROW;
                drawArrows[0] = UP_RESTORE_ARROW;
                currentSashInfo.restoreWeight = NO_WEIGHT;    // Since we are in the middle, there is no restoreWeight. We've could of been dragged here.
                currentSashInfo.sashBorderLeft = sashBorders != null && sashBorders[0];
                currentSashInfo.sashBorderRight = sashBorders != null && sashBorders[1];
            }
        } else {
            if (slammed == SLAMMED_TO_TOP) {
                addArrows = new int[1];
                drawArrows = new int[1];

                addArrows[0] = DOWN_RESTORE_ARROW;
                drawArrows[0] = DOWN_RESTORE_ARROW;
                currentSashInfo.sashBorderLeft = false;
                currentSashInfo.sashBorderRight = sashBorders != null && sashBorders[1];
            } else if (slammed == SLAMMED_TO_BOTTOM) {
                addArrows = new int[1];
                drawArrows = new int[1];

                addArrows[0] = UP_RESTORE_ARROW;
                drawArrows[0] = UP_RESTORE_ARROW;
                currentSashInfo.sashBorderLeft = sashBorders != null && sashBorders[0];
                currentSashInfo.sashBorderRight = false;
            } else {
                //Not slammed
                addArrows = new int[2];
                drawArrows = new int[2];

                addArrows[0] = UP_HIDE_ARROW;
                drawArrows[0] = UP_RESTORE_ARROW;
                addArrows[1] = DOWN_HIDE_ARROW;
                drawArrows[1] = DOWN_RESTORE_ARROW;
                currentSashInfo.restoreWeight = NO_WEIGHT;    // Since we are in the middle, there is no restoreWeight. We've could of been dragged here.
                currentSashInfo.sashBorderLeft = sashBorders != null && sashBorders[0];
                currentSashInfo.sashBorderRight = sashBorders != null && sashBorders[1];
            }
        }
        getNewSashArray(currentSashInfo, addArrows, drawArrows);

        // Need to schedule a redraw (?) because it has already drawn the old ones during the set bounds in super layout.
        //currentSashInfo.sash.redraw();
    }

    protected void upRestoreClicked(SashInfo sashinfo) {
        // This means restore just the sash below restoreWeight and reduce the above restoreWeight by the right amount.
        int[] weights = getWeights();

        weights[0] = 1000 - sashinfo.restoreWeight;    // Assume weights are always in units of 1000.
        weights[1] = sashinfo.restoreWeight;
        sashinfo.restoreWeight = NO_WEIGHT;

        try {
            setWeights(weights);
        } catch (SWTError e) {
            // Sometimes it happens if sash controls aren't yet created
            // Just ignore
        }
        fireDividerMoved();
    }

    protected void upHideClicked(SashInfo sashinfo) {
        int[] weights = getWeights();
        if (weights.length != 2) {
            return;
        }

        // Up hide, so save the current restoreWeight of 1 into the sash info, and move to the top.
        if (currentSashInfo.restoreWeight == NO_WEIGHT) {
            currentSashInfo.restoreWeight = weights[1];    // Not currently maxed, save position.

            saveChildControlSizes();
        }
        weights[0] = 0;
        weights[1] = 1000;

        // If the upper panel has focus, flip focus to the lower panel because the upper panel is now hidden.
        Control[] children = getChildren();
        boolean upperFocus = isFocusAncestorA(children[0]);
        try {
            setWeights(weights);
        } catch (SWTError e) {
            log.debug(e);
        }
        setWeights(weights);
        if (upperFocus)
            children[1].setFocus();
        fireDividerMoved();
    }

    protected void downRestoreClicked(SashInfo sashinfo) {
        // This means restore just the sash below restoreWeight and increase the above restoreWeight by that amount.
        int[] weights = getWeights();

        weights[0] = 1000 - sashinfo.restoreWeight;    // Assume weights are always in units of 1000.
        weights[1] = sashinfo.restoreWeight;
        sashinfo.restoreWeight = NO_WEIGHT;

        setWeights(weights);
        fireDividerMoved();
    }

    protected void downHideClicked() {
        int[] weights = getWeights();

        // Down hide, so save the current restoreWeight of 1 into the sash info, and move to the bottom.
        if (currentSashInfo.restoreWeight == NO_WEIGHT) {
            currentSashInfo.restoreWeight = weights[1];    // Not currently maxed, save current restoreWeight.
            saveChildControlSizes();
        }
        weights[0] = 1000;
        weights[1] = 0;

        // If the lower panel has focus, flip focus to the upper panel because the lower panel is now hidden.
        Control[] children = getChildren();
        boolean lowerFocus = isFocusAncestorA(children[1]);
        setWeights(weights);
        if (lowerFocus)
            children[0].setFocus();
        fireDividerMoved();
    }

    /*
     * Helper method for upHideClicked / downHideClicked
     */
    private void saveChildControlSizes() {
        // Save control sizes
        Control[] children = getChildren();
        int iChildToSave = 0;
        for (int i = 0; i < children.length && iChildToSave < 2; i++) {
            Control child = children[i];
            if (!(child instanceof Sash)) {
                currentSashInfo.savedSizes[iChildToSave] = child.getSize();
                iChildToSave++;
            }
        }
    }

    /*
     * This determines if the control or one of its children
     * has the focus. Control.isFocusAncestor is hidden by SWT, but it is really useful.
     */
    protected boolean isFocusAncestorA(Control control) {
        Display display = getDisplay();
        Control focusControl = display.getFocusControl();
        while (focusControl != null && focusControl != control) {
            focusControl = focusControl.getParent();
        }
        return control == focusControl;
    }

    protected void getNewSashArray(SashInfo sashInfo, int[] addArrowTypes, int[] drawArrowTypes) {

//		int[][] thisSash = sashInfo.sashLocs;
//		if (thisSash == null) 
        sashInfo.sashLocs = new int[addArrowTypes.length][];
        int[][] thisSash = sashInfo.sashLocs;

        int aSize = ARROW_WIDTH;    // Width of arrow
        int tSize = aSize + 2 * ARROW_MARGIN;        // Total Width (arrow + margin)
        int neededSize = tSize * addArrowTypes.length;

        boolean vertical = getOrientation() == SWT.VERTICAL;
        Point s = sashInfo.sash.getSize();
        int start;
        int x;
        int y;
        int width;
        int height;
        if (vertical) {
            start = (s.x - neededSize) / 2;
            x = start;
            y = (s.y - ARROW_HEIGHT) / 2;    // Center vertically, no margin required.
            width = tSize;
            height = aSize;
        } else {
            start = (s.y - neededSize) / 2;
            y = start;
            x = (s.x - ARROW_HEIGHT) / 2;    // Center horizontally, no margin required.
            width = aSize;
            height = tSize;
        }
        for (int j = 0; j < addArrowTypes.length; j++) {
            if (thisSash[j] == null)
                thisSash[j] = new int[]{addArrowTypes[j], drawArrowTypes[j], x, y, width, height};
            else {
                // Reuse the array
                thisSash[j][ARROW_TYPE_INDEX] = addArrowTypes[j];
                thisSash[j][ARROW_DRAWN_INDEX] = drawArrowTypes[j];
                thisSash[j][X_INDEX] = x;
                thisSash[j][Y_INDEX] = y;
                thisSash[j][WIDTH_INDEX] = width;
                thisSash[j][HEIGHT_INDEX] = height;
            }
            if (vertical)
                x += tSize;
            else
                y += tSize;
        }
    }

    protected void drawSashBorder(GC gc, SashInfo sashInfo) {
        gc.setForeground(arrowColor);
        gc.setLineStyle(SWT.LINE_SOLID);
        Point s = sashInfo.sash.getSize();
        int[][] sashLocs = sashInfo.sashLocs;
        int lastLocIndex = sashLocs.length - 1;

        int sashDim = getSashWidth();
        if (getOrientation() == SWT.VERTICAL) {
            int leftArrowPos = sashLocs[0][2] - ARROW_MARGIN;
            int rightArrowPos = sashLocs[lastLocIndex][2] + sashLocs[lastLocIndex][5] + ARROW_MARGIN * 3;

            gc.drawLine(0,  sashDim / 2, leftArrowPos, sashDim / 2);
            gc.drawLine(rightArrowPos,  sashDim / 2, s.x - 1, sashDim / 2);
            gc.drawRoundRectangle(leftArrowPos, 0, rightArrowPos - leftArrowPos, sashDim - 1, 3, 3);
        } else {
            int topArrowPos = sashLocs[0][3] - ARROW_MARGIN;
            int bottomArrowPos = sashLocs[lastLocIndex][3] + sashLocs[lastLocIndex][4] + ARROW_MARGIN * 3;

            gc.drawLine(sashDim / 2, 0, sashDim / 2, topArrowPos);
            gc.drawLine(sashDim / 2, bottomArrowPos, sashDim / 2, s.y - 1);
            gc.drawRoundRectangle(0, topArrowPos, sashDim - 1, bottomArrowPos - topArrowPos, 3, 3);
        }
    }

    protected void drawArrow(GC gc, int[] sashLoc, boolean selected, boolean isSlammed) {
        int oldAntialias = gc.getAntialias();
        gc.setAntialias(SWT.ON);

        int indent = 0;
        if (selected) {
            if (!inMouseClick) {
                // Draw the selection box.
                Color highlightShadow = getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);
                Color normalShadow = getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
                gc.setForeground(highlightShadow);
                gc.drawLine(sashLoc[X_INDEX], sashLoc[Y_INDEX] + sashLoc[HEIGHT_INDEX], sashLoc[X_INDEX], sashLoc[Y_INDEX]);
                gc.drawLine(sashLoc[X_INDEX], sashLoc[Y_INDEX], sashLoc[X_INDEX] + sashLoc[WIDTH_INDEX], sashLoc[Y_INDEX]);

                //gc.setForeground(normalShadow);
                //gc.drawLine(sashLoc[X_INDEX], sashLoc[Y_INDEX] + sashLoc[HEIGHT_INDEX],
                //  sashLoc[X_INDEX] + sashLoc[WIDTH_INDEX], sashLoc[Y_INDEX] + sashLoc[HEIGHT_INDEX]);
                //gc.drawLine(sashLoc[X_INDEX] + sashLoc[WIDTH_INDEX], sashLoc[Y_INDEX] + sashLoc[HEIGHT_INDEX],
                //  sashLoc[X_INDEX] + sashLoc[WIDTH_INDEX], sashLoc[Y_INDEX]);
            } else {
                // Draw pushed selection box.
                indent = 1;
                Color highlightShadow = getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);
                Color normalShadow = getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
                gc.setForeground(normalShadow);
                gc.drawLine(sashLoc[X_INDEX], sashLoc[Y_INDEX] + sashLoc[HEIGHT_INDEX], sashLoc[X_INDEX], sashLoc[Y_INDEX]);
                gc.drawLine(sashLoc[X_INDEX], sashLoc[Y_INDEX], sashLoc[X_INDEX] + sashLoc[WIDTH_INDEX], sashLoc[Y_INDEX]);

                //gc.setForeground(highlightShadow);
                //gc.drawLine(sashLoc[X_INDEX], sashLoc[Y_INDEX] + sashLoc[HEIGHT_INDEX],
                //  sashLoc[X_INDEX] + sashLoc[WIDTH_INDEX], sashLoc[Y_INDEX] + sashLoc[HEIGHT_INDEX]);
                //gc.drawLine(sashLoc[X_INDEX] + sashLoc[WIDTH_INDEX],
                //  sashLoc[Y_INDEX] + sashLoc[HEIGHT_INDEX], sashLoc[X_INDEX] + sashLoc[WIDTH_INDEX], sashLoc[Y_INDEX]);
            }
        }

        if (getOrientation() == SWT.VERTICAL) {
            switch (sashLoc[ARROW_DRAWN_INDEX]) {
                case UP_RESTORE_ARROW:
                case DOWN_HIDE_ARROW:
                    drawUpArrow(gc, sashLoc[X_INDEX] + indent, sashLoc[Y_INDEX] + indent);
                    break;
                case DOWN_RESTORE_ARROW:
                case UP_HIDE_ARROW:
                    drawDownArrow(gc, sashLoc[X_INDEX] + indent, sashLoc[Y_INDEX] + indent);
                    break;
            }
        } else {
            switch (sashLoc[ARROW_DRAWN_INDEX]) {
                case UP_RESTORE_ARROW:
                case DOWN_HIDE_ARROW:
                    drawLeftArrow(gc, sashLoc[X_INDEX] + indent, sashLoc[Y_INDEX] + indent);
                    break;
                case DOWN_RESTORE_ARROW:
                case UP_HIDE_ARROW:
                    drawRightArrow(gc, sashLoc[X_INDEX] + indent, sashLoc[Y_INDEX] + indent);
                    break;
            }
        }

        if (oldAntialias != SWT.ON) {
            gc.setAntialias(oldAntialias);
        }
    }

    // Draw at the given x/y (upper left corner of arrow area).
    protected void drawUpArrow(GC gc, int x, int y) {
        gc.setForeground(arrowColor);
        gc.setBackground(arrowColor);
        int[] triangle = new int[] {
            x + ARROW_MARGIN, y + ARROW_HEIGHT - 1,
            x + ARROW_MARGIN + ARROW_WIDTH, y + ARROW_HEIGHT - 1,
            x + ARROW_MARGIN + ARROW_WIDTH/2, y + 1
        };

        gc.drawPolygon(triangle);
        gc.fillPolygon(triangle);
    }

    // Draw at the given x/y (upper left corner of arrow area).
    protected void drawDownArrow(GC gc, int x, int y) {
        gc.setForeground(arrowColor);
        gc.setBackground(arrowColor);
        int[] triangle = new int[] {
            x + ARROW_MARGIN - 1, y + 1,
            x + ARROW_MARGIN + ARROW_WIDTH - 1, y + 1,
            x + ARROW_MARGIN + ARROW_WIDTH/2 - 1, y + ARROW_HEIGHT - 1
        };

        gc.drawPolygon(triangle);
        gc.fillPolygon(triangle);
    }

    // Draw at the given x/y (upper left corner of arrow area).
    protected void drawLeftArrow(GC gc, int x, int y) {
        gc.setForeground(arrowColor);
        gc.setBackground(arrowColor);
        int[] triangle = new int[] {
            x + ARROW_HEIGHT - 1, y + ARROW_MARGIN,
            x + ARROW_HEIGHT - 1, y + ARROW_MARGIN + ARROW_WIDTH,
            x + 1, y + ARROW_MARGIN + ARROW_WIDTH/2
        };

        gc.drawPolygon(triangle);
        gc.fillPolygon(triangle);
    }

    // Draw at the given x/y (upper left corner of arrow area).
    protected void drawRightArrow(GC gc, int x, int y) {
        gc.setForeground(arrowColor);
        gc.setBackground(arrowColor);
        int[] triangle = new int[] {
            x + 1, y + ARROW_MARGIN,
            x + 1, y + ARROW_MARGIN + ARROW_WIDTH,
            x + ARROW_HEIGHT - 1, y + ARROW_MARGIN + ARROW_WIDTH/2
        };

        gc.drawPolygon(triangle);
        gc.fillPolygon(triangle);
    }

    public int getRestoreWeight() {
        if (currentSashInfo != null)
            return currentSashInfo.restoreWeight;
        else
            return -1;
    }

    public void setRestoreWeight(int weight) {
        if (weight >= 0 && currentSashInfo != null) {
            //recomputeSashInfo();
            currentSashInfo.restoreWeight = weight;
        }
    }

    public Point[] getSavedSizes() {
        if (currentSashInfo != null) {
            return currentSashInfo.savedSizes;
        } else {
            return null;
        }
    }

    /**
     * Adds a custom sashform listener. This listener will be removed when
     * this control is disposed.
     *
     * @since 1.2.0
     */
    public void addCustomSashFormListener(ICustomSashFormListener listener) {
        if (customSashFormListeners == null)
            customSashFormListeners = new ArrayList<>();
        customSashFormListeners.add(listener);
    }

    protected void fireDividerMoved() {
        if (customSashFormListeners != null && customSashFormListeners.size() > 0) {
            int[] weights = getWeights();
            if (weights != null && weights.length == 2) {
                int firstControlWeight = weights[0];
                int secondControlWeight = weights[1];
                for (ICustomSashFormListener listener : customSashFormListeners) {
                    listener.dividerMoved(firstControlWeight, secondControlWeight);
                }
            }
        }
    }

}
