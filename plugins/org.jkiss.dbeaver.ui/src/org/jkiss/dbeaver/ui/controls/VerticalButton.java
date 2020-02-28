/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.dbeaver.ui.ActionUtils;
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

    private boolean isHover;

    private IAction action;
    private IServiceLocator serviceLocator;
    private String commandId;
    private boolean checked;
    //float[] angles = {0, 90, 180, 270};
    //int index = 0;

    public VerticalButton(VerticalFolder parent, int style) {
        super(parent, style | SWT.NO_FOCUS);

        setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
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
                    event.widget = event.item = VerticalButton.this;
                    runAction(event);
                }
                hit = false;
            }
        });
        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == '\r' || e.character == ' ') {
                    Event event = new Event();
                    event.widget = event.item = VerticalButton.this;
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
        } else if (commandId != null) {
            ActionUtils.runCommand(commandId, serviceLocator);
        }
    }

    public VerticalFolder getFolder() {
        return (VerticalFolder) getParent();
    }

    public String getText() {
        return text;
    }

    public void setText(String string) {
        this.text = string;
        redraw();
    }

    public void setImage(Image image) {
        this.image = image;
        redraw();
    }

    private void setImage(ImageDescriptor imageDescriptor) {
        if (imageDescriptor != null) {
            this.image = imageDescriptor.createImage(true);
            addDisposeListener(e -> {
                UIUtils.dispose(image);
            });
        }
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        GC gc = new GC(this);
        try {
            return computeSize(gc, wHint, hHint, changed);
        } finally {
            gc.dispose();
        }
    }

    public Point computeSize(GC gc, int wHint, int hHint, boolean changed) {
        Point textSize = gc.stringExtent(getText());

        Point iconSize = new Point(0, 0);
        if (image != null) {
            Rectangle imageBounds = image.getBounds();
            iconSize.x = imageBounds.width + BORDER_MARGIN;
            iconSize.y = imageBounds.height + BORDER_MARGIN * 2;
        }

        if (CommonUtils.isEmpty(text)) {
            return new Point(
                iconSize.x,
                iconSize.y);
        } else {
            return new Point(
                Math.max(iconSize.y, textSize.y + BORDER_MARGIN * 2),
                textSize.x + (BORDER_MARGIN + VERT_INDENT) * 2 + iconSize.x);
        }
    }

    public void paint(PaintEvent e) {
        boolean selected = isSelected();
        Point size = computeSize(e.gc, -1, -1, false);

        if (selected || isHover) {
            Color curBackground = e.gc.getBackground();
            boolean isDarkBG = UIUtils.isDark(curBackground.getRGB());
            RGB blendRGB = isDarkBG ? new RGB(255, 255, 255) : new RGB(0, 0, 0);

            // Make bg a bit darker
            if (isHover) {
                RGB buttonHoverRGB = UIUtils.blend(curBackground.getRGB(), blendRGB, 90);
                Color buttonHoverColor = UIUtils.getSharedTextColors().getColor(buttonHoverRGB);
                e.gc.setBackground(buttonHoverColor);
                e.gc.fillRectangle(0, 0, size.x, size.y);
            }
            if (selected) {
                if (!isHover) {
                    RGB selectedBackRGB = UIUtils.blend(curBackground.getRGB(), blendRGB, 95);
                    Color selectedBackColor = UIUtils.getSharedTextColors().getColor(selectedBackRGB);
                    e.gc.setBackground(selectedBackColor);
                    e.gc.fillRectangle(0, 0, size.x, size.y);
                }
                e.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                e.gc.drawRectangle(0, 0, size.x - 1, size.y - 1);
            }
        }

        int x = 0;

        String text = getText();
        if (!CommonUtils.isEmpty(text)) {
            Transform tr = new Transform(e.display);

            e.gc.setAntialias(SWT.ON);
            if ((getStyle() & SWT.RIGHT) == SWT.RIGHT) {
                tr.translate(size.x, 0);
                tr.rotate(90);
            } else {
                tr.translate(0, size.y);
                tr.rotate(-90);
            }
            e.gc.setTransform(tr);

            x += VERT_INDENT;
        }

        if (image != null) {
            e.gc.drawImage(image, x, BORDER_MARGIN);
            x += image.getBounds().width + BORDER_MARGIN;
        }

        e.gc.setForeground(UIStyles.getDefaultTextForeground());
        e.gc.drawString(this.text, x, BORDER_MARGIN);
    }

    private boolean isSelected() {
        return checked ||
            ((getStyle() & SWT.RADIO) == SWT.RADIO && getFolder().getSelection() == this) ||
            (action != null && (action.getStyle() & IAction.AS_CHECK_BOX) == IAction.AS_CHECK_BOX && action.isChecked());
    }

    public void addSelectionListener(SelectionListener listener) {
        addListener(SWT.Selection, event -> listener.widgetSelected(new SelectionEvent(event)));
    }

    public IAction getAction() {
        return action;
    }

    public void setAction(IAction action, boolean showText) {
        this.action = action;
        setImage(action.getImageDescriptor());
        if (showText) {
            this.text = action.getText();
        }
        String toolTipText = action.getToolTipText();
        if (!CommonUtils.isEmpty(toolTipText)) {
            this.setToolTipText(toolTipText);
        }
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommand(IServiceLocator serviceLocator, String commandId, boolean showText) {
        this.serviceLocator = serviceLocator;
        this.commandId = commandId;
        setImage(ActionUtils.findCommandImage(commandId));
        if (showText) {
            this.text = ActionUtils.findCommandName(commandId);
        }
        String toolTipText = ActionUtils.findCommandDescription(commandId, serviceLocator, false);
        if (!CommonUtils.isEmpty(toolTipText)) {
            this.setToolTipText(toolTipText);
        }
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean selection) {
        this.checked = selection;
    }

    public static VerticalButton create(VerticalFolder folder, int style, IServiceLocator serviceLocator, String commandId, boolean showText) {
        VerticalButton button = new VerticalButton(folder, style);
        button.setCommand(serviceLocator, commandId, showText);
        return button;
    }

    public static VerticalButton create(VerticalFolder folder, int style, IAction action, boolean showText) {
        VerticalButton button = new VerticalButton(folder, style);
        button.setAction(action, showText);
        return button;
    }

}