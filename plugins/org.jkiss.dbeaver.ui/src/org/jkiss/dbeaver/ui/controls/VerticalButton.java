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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

public class VerticalButton extends Canvas {

    public static final int BORDER_MARGIN = 2;
    public static final int VERT_INDENT = 8;

    private int mouse = 0;
    private boolean hit = false;

    private String text = "";
    private Image image = null;

    private float rotatingAngle = -90;
    private boolean isHover;

    private IAction action;
    //float[] angles = {0, 90, 180, 270};
    //int index = 0;

    public VerticalButton(VerticalFolder parent, int style) {
        super(parent, style);
        parent.addItem(this);

        this.addPaintListener(this::paint);
        addMouseMoveListener(e -> {
            if (!isHover) {
                isHover = true;
                redraw();
            }
        });
        this.addMouseTrackListener(new MouseTrackAdapter() {
            public void mouseEnter(MouseEvent e) {
                isHover = true;
                redraw();
            }

            public void mouseExit(MouseEvent e) {
                isHover = false;
                redraw();
            }

            @Override
            public void mouseHover(MouseEvent e) {
                if (!isHover) {
                    isHover = true;
                    redraw();
                }
            }
        });
        this.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent e) {
                isHover = true;
                hit = true;
                redraw();
            }

            public void mouseUp(MouseEvent e) {
                isHover = true;
                redraw();
                if (hit) {
                    Event event = new Event();
                    event.widget = VerticalButton.this;
                    runAction(event);
                }
                hit = false;
            }
        });
        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == '\r' || e.character == ' ') {
                    Event event = new Event();
                    runAction(event);
                }
            }
        });

        this.addDisposeListener(e -> getFolder().removeItem(this));
    }

    private void runAction(Event event) {
        notifyListeners(SWT.Selection, event);
        if ((getStyle() & SWT.RADIO) == SWT.RADIO) {
            getFolder().setSelection(this);
        }
        if (action != null) {
            action.runWithEvent(event);
            redraw();
        }
    }

    public VerticalFolder getFolder() {
        return (VerticalFolder) getParent();
    }

    public void setText(String string) {
        this.text = string;
        redraw();
    }

    public void setImage(Image image) {
        this.image = image;
        redraw();
    }


    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        GC gc = new GC(this);
        Point textSize = gc.stringExtent(text);
        gc.dispose();

        Point iconSize = new Point(0, 0);
        if (image != null) {
            Rectangle imageBounds = image.getBounds();
            iconSize.x = imageBounds.width + BORDER_MARGIN;
            iconSize.y = imageBounds.height + BORDER_MARGIN * 2;
        }

        return new Point(
            Math.max(iconSize.y, textSize.y + BORDER_MARGIN * 2),
            textSize.x + (BORDER_MARGIN + VERT_INDENT) * 2 + iconSize.x);
    }

    public void paint(PaintEvent e) {
        boolean selected = isSelected();
        if (selected || isHover) {
            Color curBackground = e.gc.getBackground();

            // Make bg a bit darker
            if (isHover) {
                RGB buttonHoverRGB = UIUtils.blend(curBackground.getRGB(), new RGB(0, 0, 0), 90);
                Color buttonHoverColor = UIUtils.getSharedTextColors().getColor(buttonHoverRGB);
                e.gc.setBackground(buttonHoverColor);
                e.gc.fillRectangle(e.x, e.y, e.width, e.height);
            }
            if (selected) {
                if (!isHover) {
                    RGB selectedBackRGB = UIUtils.blend(curBackground.getRGB(), new RGB(0, 0, 0), 95);
                    Color selectedBackColor = UIUtils.getSharedTextColors().getColor(selectedBackRGB);
                    e.gc.setBackground(selectedBackColor);
                    e.gc.fillRectangle(e.x, e.y, e.width, e.height);
                }
                e.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                e.gc.drawRectangle(e.x, e.y, e.width - 1, e.height - 1);
            }
        }
        Transform tr = new Transform(e.display);

        e.gc.setAntialias(SWT.ON);
        tr.translate(0, e.height);
        tr.rotate(rotatingAngle);
        tr.invert();
        e.gc.setTransform(tr);

        int x = e.x + VERT_INDENT;
        if (image != null) {
            e.gc.drawImage(image, x, e.y + BORDER_MARGIN);
            x += image.getBounds().width + BORDER_MARGIN;
        }

        e.gc.setForeground(UIStyles.getDefaultTextForeground());
        e.gc.drawString(text, x, e.y + BORDER_MARGIN);
    }

    private boolean isSelected() {
        return ((getStyle() & SWT.RADIO) == SWT.RADIO && getFolder().getSelection() == this) ||
            (action != null && (action.getStyle() & IAction.AS_CHECK_BOX) == IAction.AS_CHECK_BOX && action.isChecked());
    }

    public void addSelectionListener(SelectionListener listener) {
        addListener(SWT.Selection, event -> listener.widgetSelected(new SelectionEvent(event)));
    }

    public void setAction(IAction action) {
        this.action = action;
        ImageDescriptor imageDescriptor = action.getImageDescriptor();
        if (imageDescriptor != null) {
            this.image = imageDescriptor.createImage(true);
            addDisposeListener(e -> UIUtils.dispose(image));
        }
        this.text = action.getText();
        String toolTipText = action.getToolTipText();
        if (!CommonUtils.isEmpty(toolTipText)) {
            this.setToolTipText(toolTipText);
        }
    }
}