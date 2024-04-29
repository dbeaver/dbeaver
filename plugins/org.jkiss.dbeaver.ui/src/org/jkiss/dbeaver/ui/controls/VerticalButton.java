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

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ICommandListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.awt.*;

public class VerticalButton extends Canvas {

    private static final Insets BASE_MARGIN = new Insets(2, 2, 2, 2);
    private static final Insets TEXT_MARGIN = new Insets(4, 0, 4, 0);
    private static final int TEXT_SPACING = TEXT_MARGIN.bottom;

    private boolean hit = false;

    private String text = "";
    private Image image = null;
    private Image imageDisabled = null;

    private boolean isHover;

    private IAction action;
    private IServiceLocator serviceLocator;
    private String commandId;
    private ICommandListener commandListener;
    private boolean checked;

    public VerticalButton(VerticalFolder parent, int style) {
        super(parent, style | SWT.NO_FOCUS);

        setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        setFont(parent.getFont());
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

        this.addDisposeListener(e -> {
            getFolder().removeItem(this);
            if (commandId != null) {
                removeCommandListener(commandId);
            }
        });
    }

    private void runAction(Event event) {
        notifyListeners(SWT.Selection, event);
        if ((getStyle() & SWT.RADIO) == SWT.RADIO) {
            getFolder().setSelection(this);
        }
        if (action != null) {
            if (action.isEnabled()) {
                action.runWithEvent(event);
                redraw();
            }
        } else if (commandId != null) {
            if (ActionUtils.isCommandEnabled(commandId, serviceLocator)) {
                ActionUtils.runCommand(commandId, serviceLocator);
            }
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
            return computeSize(gc);
        } finally {
            gc.dispose();
        }
    }

    @NotNull
    private Point computeSize(@NotNull GC gc) {
        final Point size = new Point(0, 0);

        if (image != null) {
            Rectangle imageBounds = image.getBounds();
            size.x = imageBounds.width;
            size.y = imageBounds.height;
        }

        if (!CommonUtils.isEmpty(text)) {
            Point extent = gc.stringExtent(text);
            size.x = size.x + extent.x;
            size.y = Math.max(size.y, extent.y);

            // Extra margins so the text is not too close to the border
            size.x += TEXT_MARGIN.top + TEXT_MARGIN.bottom;
            size.y += TEXT_MARGIN.left + TEXT_MARGIN.right;

            if (image != null) {
                size.x += TEXT_SPACING;
            }
        }

        return new Point(
            size.y + BASE_MARGIN.left + BASE_MARGIN.right,
            size.x + BASE_MARGIN.top + BASE_MARGIN.bottom
        );
    }

    private void paint(@NotNull PaintEvent e) {
        boolean selected = isSelected();
        Point size = computeSize(e.gc);
        Color curBackground = e.gc.getBackground();
        boolean isDarkBG = UIUtils.isDark(curBackground.getRGB());

        boolean enabled = true;
        if (getFolder().isCheckCommandEnablement()) {
            if (action != null && !action.isEnabled()) {
                enabled = false;
            } else if (commandId != null) {
                enabled = ActionUtils.isCommandEnabled(commandId, serviceLocator);
            }
        }
        if (enabled && (selected || isHover)) {

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

        int offset = BASE_MARGIN.top;

        if (!CommonUtils.isEmpty(text)) {
            offset += TEXT_MARGIN.top;
        }

        Transform transform = null;

        String text = getText();
        if (!CommonUtils.isEmpty(text)) {
            transform = new Transform(e.display);

            if ((getStyle() & SWT.RIGHT) == SWT.RIGHT) {
                transform.translate(size.x, 0);
                transform.rotate(90);
            } else {
                transform.translate(0, size.y);
                transform.rotate(-90);
            }
            e.gc.setTransform(transform);
        }

        if (image != null) {
            final Image image;
            if (!enabled) {
                if (imageDisabled == null) {
                    imageDisabled = new Image(e.display, this.image, SWT.IMAGE_GRAY);
                    addDisposeListener(e1 -> imageDisabled.dispose());
                }
                image = imageDisabled;
            } else {
                image = this.image;
            }

            final Rectangle bounds = image.getBounds();
            if (transform != null) {
                e.gc.drawImage(image, offset, (size.x - bounds.height) / 2);
                offset += bounds.height + TEXT_SPACING;
            } else {
                e.gc.drawImage(image, (size.x - bounds.width) / 2, offset);
                offset += bounds.width + TEXT_SPACING;
            }
        }

        if (!CommonUtils.isEmpty(text)) {
            final Point bounds = e.gc.textExtent(text);
            e.gc.setAntialias(SWT.ON);
            e.gc.setForeground(isDarkBG ? UIUtils.COLOR_WHITE : UIStyles.getDefaultTextForeground());
            e.gc.drawString(this.text, offset, (size.x - bounds.y) / 2);
        }

        if (transform != null) {
            transform.dispose();
        }
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
        if (this.commandId != null) {
            this.removeCommandListener(this.commandId);
        }
        this.setCommandListener(commandId);
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

    private void setCommandListener(@NotNull String commandId) {
        final Command command = ActionUtils.findCommand(commandId);
        if (command != null) {
            command.addCommandListener(commandListener = event -> {
                // Update visuals
                final String toolTipText = ActionUtils.findCommandDescription(commandId, serviceLocator, false);
                if (CommonUtils.isNotEmpty(toolTipText)) {
                    setToolTipText(toolTipText);
                }
            });
        }
    }

    private void removeCommandListener(@NotNull String commandId) {
        final Command command = ActionUtils.findCommand(commandId);
        if (command != null && commandListener != null) {
            command.removeCommandListener(commandListener);
            commandListener = null;
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