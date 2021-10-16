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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ICommandListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

public class VerticalButton extends Canvas {

    public static final int BORDER_MARGIN = 2;
    public static final int VERT_INDENT = 8;

    private static final Point EMPTY_SIZE = new Point(0, 0);

    // Transform bug in SWT appeared in 2021-06 and was fixed in 2021-09
    private static final boolean IS_TRANSFORM_BUG_PRESENT = false;

    private int mouse = 0;
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
            return computeSize(gc, wHint, hHint, changed);
        } finally {
            gc.dispose();
        }
    }

    public Point computeSize(GC gc, int wHint, int hHint, boolean changed) {
        String text = getText();
        Point textSize = CommonUtils.isEmpty(text) ? EMPTY_SIZE : gc.stringExtent(text);

        Point iconSize = EMPTY_SIZE;
        if (image != null) {
            Rectangle imageBounds = image.getBounds();
            iconSize = new Point(imageBounds.width + BORDER_MARGIN, imageBounds.height + BORDER_MARGIN * 2);
            if (textSize == EMPTY_SIZE) {
                return iconSize;
            }
        }

        return new Point(
            Math.max(iconSize.y, textSize.y + BORDER_MARGIN * 2),
            textSize.x + (BORDER_MARGIN + VERT_INDENT) * 2 + iconSize.x);
    }

    public void paint(PaintEvent e) {
        boolean selected = isSelected();
        Point size = computeSize(e.gc, -1, -1, false);

        boolean enabled = true;
        if (getFolder().isCheckCommandEnablement()) {
            if (action != null && !action.isEnabled()) {
                enabled = false;
            } else if (commandId != null) {
                enabled = ActionUtils.isCommandEnabled(commandId, serviceLocator);
            }
        }
        if (enabled && (selected || isHover)) {
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

        // In fact X and Y offsets are reversed because of transform
        int xOffset = 0;
        int yOffset = BORDER_MARGIN;

        String text = getText();
        if (!CommonUtils.isEmpty(text)) {
            // Offset shift. Windows only? (14048)
            boolean shiftOffset = IS_TRANSFORM_BUG_PRESENT && RuntimeUtils.isWindows() && (DPIUtil.getDeviceZoom() >= 200);

            Transform tr = new Transform(e.display);

            e.gc.setAntialias(SWT.ON);
            if ((getStyle() & SWT.RIGHT) == SWT.RIGHT) {
                tr.translate(size.x, 0);
                tr.rotate(90);
                if (shiftOffset) {
                    yOffset -= size.x / 2;
                }
            } else {
                tr.translate(0, size.y);
                tr.rotate(-90);
                if (shiftOffset) {
                    xOffset -= size.y / 2;
                }
            }
            e.gc.setTransform(tr);

            xOffset += VERT_INDENT;
        }

        if (image != null) {
            if (!enabled) {
                if (imageDisabled == null) {
                    imageDisabled = new Image(e.display, image, SWT.IMAGE_GRAY);
                    addDisposeListener(e1 -> imageDisabled.dispose());
                }
                e.gc.drawImage(imageDisabled, xOffset, yOffset);
            } else {
                e.gc.drawImage(image, xOffset, yOffset);
            }
            xOffset += image.getBounds().width + BORDER_MARGIN;
        }

        if (!CommonUtils.isEmpty(text)) {
            e.gc.setForeground(UIStyles.getDefaultTextForeground());
            e.gc.drawString(this.text, xOffset, yOffset);
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