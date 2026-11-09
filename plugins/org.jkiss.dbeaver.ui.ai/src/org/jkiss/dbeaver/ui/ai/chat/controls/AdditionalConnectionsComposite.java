/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatIcons;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class AdditionalConnectionsComposite extends Composite {
    private static final int CHIP_HEIGHT = 22;
    private static final int CHIP_MAX_WIDTH = 150;
    private static final int NAME_MAX_WIDTH = 100;
    private static final int CHIP_CONTENT_WIDTH = CHIP_MAX_WIDTH - NAME_MAX_WIDTH;
    private static final int SPACING = 5;
    private static final int COLLAPSED_ROWS = 2;
    private static final String CONNECTION_ID = "connectionId";

    private final AIChatControl chat;
    private final List<Canvas> chips = new ArrayList<>();
    private final Composite toggleControl;
    private final Label counter;
    private final ToolItem toggle;
    private AIChatConversation conversation;
    private final Map<RGB, Image> closeImages = new HashMap<>();
    private boolean expanded;

    AdditionalConnectionsComposite(@NotNull AIChatControl chat, @NotNull Composite parent) {
        super(parent, SWT.NONE);
        this.chat = chat;
        toggleControl = new Composite(this, SWT.NONE);
        GridLayout toggleLayout = new GridLayout(2, false);
        toggleLayout.marginWidth = 0;
        toggleLayout.marginHeight = 0;
        toggleLayout.horizontalSpacing = 0;
        toggleControl.setLayout(toggleLayout);
        counter = new Label(toggleControl, SWT.NONE);
        counter.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        ToolBar toggleBar = new ToolBar(toggleControl, SWT.FLAT);
        toggleBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        toggle = new ToolItem(toggleBar, SWT.PUSH);
        toggle.setImage(DBeaverIcons.getImage(UIIcon.ARROW_DOWN));
        Listener toggleListener = event -> {
            expanded = !expanded;
            toggle.setImage(DBeaverIcons.getImage(expanded ? UIIcon.ARROW_UP : UIIcon.ARROW_DOWN));
            requestLayout();
        };
        counter.addListener(SWT.MouseUp, event -> {
            if (event.button == 1 && counter.isEnabled()) {
                toggleListener.handleEvent(event);
            }
        });
        toggle.addListener(SWT.Selection, toggleListener);
        addDisposeListener(event -> {
            closeImages.values().forEach(Image::dispose);
        });
        setLayout(new Layout() {
            @NotNull
            @Override
            protected Point computeSize(@NotNull Composite composite, int widthHint, int heightHint, boolean flushCache) {
                int width = widthHint == SWT.DEFAULT ? 398 : widthHint;
                int height = arrange(width, false);
                return new Point(width, heightHint == SWT.DEFAULT ? height : heightHint);
            }

            @Override
            protected void layout(@NotNull Composite composite, boolean flushCache) {
                arrange(composite.getClientArea().width, true);
            }
        });
    }

    void setConversation(@NotNull AIChatConversation conversation) {
        int focusedIndex = chips.indexOf(getDisplay().getFocusControl());
        final Object focusedId = focusedIndex < 0 ? null : chips.get(focusedIndex).getData(CONNECTION_ID);
        for (Canvas chip : chips) {
            chip.dispose();
        }
        chips.clear();
        if (this.conversation != conversation) {
            expanded = false;
        }
        this.conversation = conversation;
        List<DBPDataSourceContainer> sources = conversation.getDataSources();
        for (int index = 1; index < sources.size(); index++) {
            createChip(sources.get(index));
        }
        updateCounter(chips.size());
        toggle.setImage(DBeaverIcons.getImage(expanded ? UIIcon.ARROW_UP : UIIcon.ARROW_DOWN));
        if (getLayoutData() instanceof GridData data) {
            data.exclude = chips.isEmpty();
        }
        setVisible(!chips.isEmpty());
        requestLayout();
        if (focusedIndex >= 0) {
            layout(true, true);
            Canvas next = chips.stream().filter(chip -> focusedId.equals(chip.getData(CONNECTION_ID))).findFirst().orElse(null);
            if (next != null && next.isVisible()) {
                next.setFocus();
            } else {
                boolean restored = false;
                for (int index = Math.min(focusedIndex, chips.size() - 1); index >= 0; index--) {
                    if (chips.get(index).isVisible() && chips.get(index).setFocus()) {
                        restored = true;
                        break;
                    }
                }
                if (!restored) {
                    chat.focusContextBar();
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Canvas chip : chips) {
            chip.setEnabled(enabled);
            chip.redraw();
        }
        toggleControl.setEnabled(enabled);
    }

    private void createChip(@NotNull DBPDataSourceContainer container) {
        Canvas chip = new Canvas(this, SWT.DOUBLE_BUFFERED);
        final boolean[] removePressed = {false};
        chip.addListener(SWT.Resize, event -> updateNameTooltip(chip, container));
        int textWidth;
        GC measuringGc = new GC(chip);
        try {
            textWidth = measuringGc.textExtent(container.getName()).x;
        } finally {
            measuringGc.dispose();
        }
        chip.setData(Math.min(textWidth, NAME_MAX_WIDTH) + CHIP_CONTENT_WIDTH);
        chip.setData(CONNECTION_ID, container.getId());
        chip.addPaintListener(event -> {
            GC gc = event.gc;
            Rectangle bounds = chip.getClientArea();
            DBPConnectionConfiguration configuration = container.getConnectionConfiguration();
            Color background = CommonUtils.isEmpty(configuration.getConnectionColor())
                && DBPConnectionType.DEV.getId().equals(configuration.getConnectionType().getId())
                ? null : UIUtils.getConnectionColor(configuration);
            if (background == null) {
                background = UIStyles.getDefaultWidgetBackground();
            }
            gc.setBackground(background);
            gc.fillRoundRectangle(0, 0, bounds.width, bounds.height, 10, 10);
            Image icon = DBeaverIcons.getImage(container.getDriver().getIcon());
            if (icon != null) {
                gc.drawImage(icon, 4, (bounds.height - icon.getBounds().height) / 2);
            }
            Color foreground = UIStyles.getContrastColor(background);
            gc.setForeground(foreground);
            String name = fitName(gc, container.getName(), getNameWidth(bounds));
            gc.drawText(name, 24, (bounds.height - gc.textExtent(name).y) / 2, true);
            Point mouse = chip.toControl(chip.getDisplay().getCursorLocation());
            boolean removeHovered = chip.isEnabled() && isRemoveHit(bounds, mouse.x, mouse.y);
            Image close = removeHovered ? DBeaverIcons.getImage(AIChatIcons.CHIP_CLOSE_HOVER) : getCloseImage(foreground.getRGB());
            gc.drawImage(close, bounds.width - 20, (bounds.height - close.getBounds().height) / 2);
            if (chip.isFocusControl()) {
                gc.drawFocus(1, 1, bounds.width - 2, bounds.height - 2);
            }
        });
        chip.addMouseMoveListener(event -> {
            updateNameTooltip(chip, container);
            chip.redraw();
        });
        chip.addListener(SWT.MouseExit, event -> {
            chip.setToolTipText(null);
            chip.redraw();
        });
        chip.addMouseListener(MouseListener.mouseDownAdapter(event -> {
            if (event.button == 1 && chip.isEnabled()) {
                removePressed[0] = isRemoveHit(chip.getClientArea(), event.x, event.y);
                chip.setFocus();
            }
        }));
        chip.addListener(SWT.MouseUp, event -> {
            if (event.button == 1) {
                boolean remove = removePressed[0] && chip.isEnabled() && isRemoveHit(chip.getClientArea(), event.x, event.y);
                removePressed[0] = false;
                if (remove) {
                    chat.removeConnection(container.getId());
                }
            }
        });
        chip.addListener(SWT.KeyDown, event -> {
            if (event.keyCode == SWT.DEL || event.keyCode == SWT.BS || event.keyCode == SWT.CR || event.character == ' ') {
                chat.removeConnection(container.getId());
            }
        });
        chip.addListener(SWT.FocusIn, event -> chip.redraw());
        chip.addListener(SWT.FocusOut, event -> chip.redraw());
        chip.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(@NotNull AccessibleEvent event) {
                event.result = NLS.bind(AIChatMessagesUI.ai_chat_remove_connection_named, container.getName());
            }
        });
        chip.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
            @Override
            public void getRole(@NotNull AccessibleControlEvent event) {
                event.detail = ACC.ROLE_PUSHBUTTON;
            }
        });
        chat.enableConnectionDropTargets(chip);
        chips.add(chip);
    }

    private static boolean isRemoveHit(@NotNull Rectangle bounds, int mouseX, int mouseY) {
        return mouseX >= bounds.width - 22 && bounds.contains(mouseX, mouseY);
    }

    @NotNull
    private Image getCloseImage(@NotNull RGB foreground) {
        return closeImages.computeIfAbsent(foreground,
            color -> AIChatIcons.createForegroundImage(getDisplay(), AIChatIcons.CHIP_CLOSE, color));
    }

    private static void updateNameTooltip(@NotNull Canvas chip, @NotNull DBPDataSourceContainer container) {
        Rectangle bounds = chip.getClientArea();
        Point mouse = chip.toControl(chip.getDisplay().getCursorLocation());
        String tooltip = null;
        if (mouse.x >= 24 && mouse.x < bounds.width - 22 && bounds.contains(mouse)) {
            DBPConnectionConfiguration configuration = container.getConnectionConfiguration();
            String host = configuration.getHostName();
            if (host != null && !host.isBlank()) {
                String port = configuration.getHostPort();
                if (host.contains(":") && !host.startsWith("[")) {
                    host = "[" + host + "]";
                }
                tooltip = host + (port == null || port.isBlank() ? "" : ":" + port);
            }
            GC gc = new GC(chip);
            try {
                if (gc.textExtent(container.getName()).x > getNameWidth(bounds)) {
                    tooltip = container.getName() + (tooltip == null ? "" : "\n" + tooltip);
                }
            } finally {
                gc.dispose();
            }
        }
        if (!Objects.equals(chip.getToolTipText(), tooltip)) {
            chip.setToolTipText(tooltip);
        }
    }

    private static int getNameWidth(@NotNull Rectangle bounds) {
        return Math.max(0, Math.min(NAME_MAX_WIDTH, bounds.width - CHIP_CONTENT_WIDTH));
    }

    @NotNull
    private static String fitName(@NotNull GC gc, @NotNull String name, int width) {
        if (gc.textExtent(name).x <= width) {
            return name;
        }
        int end = name.length();
        while (end > 0 && gc.textExtent(name.substring(0, end) + "…").x > width) {
            end = name.offsetByCodePoints(end, -1);
        }
        return name.substring(0, end) + "…";
    }

    private int arrange(int width, boolean apply) {
        if (chips.isEmpty()) {
            if (apply) {
                toggleControl.setVisible(false);
            }
            return 0;
        }
        boolean overflow = rowCount(width) > COLLAPSED_ROWS;
        counter.setFont(chips.getFirst().getFont());
        int visible = chips.size();
        Point toggleSize = new Point(0, 0);
        int availableWidth = Math.max(1, width);
        if (overflow) {
            if (expanded) {
                toggleSize = computeToggleSize(visible);
                availableWidth = Math.max(1, width - toggleSize.x - SPACING);
            } else {
                // the visible count changes the control width, so fit the largest valid prefix
                for (visible = countVisible(width); visible > 0; visible--) {
                    toggleSize = computeToggleSize(visible);
                    availableWidth = Math.max(1, width - toggleSize.x - SPACING);
                    if (countVisible(availableWidth) >= visible) {
                        break;
                    }
                }
            }
        }
        int row = 0;
        int offset = 0;
        for (int index = 0; index < chips.size(); index++) {
            Canvas chip = chips.get(index);
            if (index >= visible) {
                if (apply) {
                    chip.setVisible(false);
                }
                continue;
            }
            int chipWidth = Math.min((int) chip.getData(), availableWidth);
            if (offset > 0 && offset + chipWidth > availableWidth) {
                offset = 0;
                row++;
            }
            if (apply) {
                chip.setVisible(true);
                chip.setBounds(offset, row * (CHIP_HEIGHT + SPACING), chipWidth, CHIP_HEIGHT);
            }
            offset += chipWidth + SPACING;
        }
        int rows = row + 1;
        int height = rows * (CHIP_HEIGHT + SPACING) - SPACING;
        if (overflow) {
            height = Math.max(height, toggleSize.y);
        }
        if (apply) {
            toggleControl.setVisible(overflow);
            updateCounter(visible);
            String tooltip = expanded ? AIChatMessagesUI.ai_chat_connections_collapse : AIChatMessagesUI.ai_chat_connections_expand;
            counter.setToolTipText(tooltip);
            toggle.setToolTipText(tooltip);
            toggleControl.setBounds(Math.max(0, width - toggleSize.x), Math.max(0, height - toggleSize.y), toggleSize.x, toggleSize.y);
            toggleControl.layout(true, true);
        }
        return height;
    }

    private void updateCounter(int visible) {
        counter.setText(NLS.bind(AIChatMessagesUI.ai_chat_connections_count, visible, chips.size()));
    }

    @NotNull
    private Point computeToggleSize(int visible) {
        String previousText = counter.getText();
        try {
            updateCounter(visible);
            return toggleControl.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        } finally {
            counter.setText(previousText);
        }
    }

    private int countVisible(int width) {
        int row = 0;
        int offset = 0;
        int visible = 0;
        for (Canvas chip : chips) {
            int chipWidth = Math.min((int) chip.getData(), Math.max(1, width));
            if (offset > 0 && offset + chipWidth > width) {
                row++;
                offset = 0;
            }
            if (row >= COLLAPSED_ROWS) {
                break;
            }
            visible++;
            offset += chipWidth + SPACING;
        }
        return visible;
    }

    private int rowCount(int width) {
        int rows = 1;
        int offset = 0;
        for (Canvas chip : chips) {
            int chipWidth = Math.min((int) chip.getData(), Math.max(1, width));
            if (offset > 0 && offset + chipWidth > width) {
                rows++;
                offset = 0;
            }
            offset += chipWidth + SPACING;
        }
        return rows;
    }
}
