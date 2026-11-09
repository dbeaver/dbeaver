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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.model.ai.utils.AIChatTimeUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatIcons;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;

import java.util.List;
import java.util.Map;
import java.util.Objects;

class ConversationSelectorPopup extends AbstractPopupPanel {
    private static final int MARKER_WIDTH = 29;
    private static final String CAPTION_WIDTH = "captionWidth";
    private final Control anchor;
    private final AIChatControl chat;
    private final Map<DBPDataSourceContainer, List<AIChatConversation>> groups;
    private Table table;
    private int dateWidth;

    ConversationSelectorPopup(
        @NotNull Control anchor,
        @NotNull AIChatControl chat,
        @NotNull Map<DBPDataSourceContainer, List<AIChatConversation>> groups
    ) {
        super(anchor.getShell(), AIChatMessagesUI.ai_chat_select_conversation);
        this.anchor = anchor;
        this.chat = chat;
        this.groups = groups;
        setModeless(true);
        setShellStyle(SWT.ON_TOP | SWT.TOOL | SWT.BORDER);
    }

    @Override
    protected boolean isShowTitle() {
        return false;
    }

    @Override
    protected boolean isResizable() {
        return false;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 6;
        layout.marginHeight = 3;
        area.setLayout(layout);
        table = new Table(area, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        TableColumn marker = new TableColumn(table, SWT.NONE);
        marker.setWidth(MARKER_WIDTH);
        new TableColumn(table, SWT.NONE);
        new TableColumn(table, SWT.RIGHT);
        Color secondary = table.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DISABLED_FOREGROUND);
        Image selected = AIChatIcons.createForegroundImage(
            table.getDisplay(), AIChatIcons.CONVERSATION_SELECTED, table.getForeground().getRGB());
        table.addDisposeListener(event -> selected.dispose());
        GC gc = new GC(table);
        try {
            for (var group : groups.entrySet()) {
                if (group.getValue().isEmpty()) {
                    continue;
                }
                TableItem heading = new TableItem(table, SWT.NONE);
                heading.setText(1, group.getKey() == null
                    ? AIChatMessagesUI.ai_chat_context_menu_no_connection : group.getKey().getName());
                heading.setForeground(secondary);
                for (AIChatConversation conversation : group.getValue()) {
                    TableItem item = new TableItem(table, SWT.NONE);
                    String timestamp = AIChatTimeUtils.format(conversation.getTime(), true);
                    item.setText(new String[]{"", conversation.getCaption(), timestamp});
                    item.setForeground(2, secondary);
                    item.setData(conversation);
                    item.setData(CAPTION_WIDTH, gc.textExtent(conversation.getCaption()).x + 8);
                    if (conversation == chat.getActiveConversation()) {
                        item.setImage(0, selected);
                    }
                    dateWidth = Math.max(dateWidth, gc.textExtent(timestamp).x + 16);
                }
            }
        } finally {
            gc.dispose();
        }
        table.addListener(SWT.Resize, event -> resizeColumns());
        table.addListener(SWT.KeyDown, event -> {
            if (event.keyCode == SWT.ESC) {
                event.doit = false;
                close();
            } else if (event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR || event.character == ' ') {
                event.doit = false;
                activateSelection();
            } else if (event.keyCode == SWT.ARROW_DOWN || event.keyCode == SWT.ARROW_UP) {
                event.doit = false;
                moveSelection(event.keyCode == SWT.ARROW_DOWN ? 1 : -1);
            }
        });
        table.addListener(SWT.MouseMove, event -> {
            TableItem item = table.getItem(new Point(event.x, event.y));
            if (item != null && item.getData() instanceof AIChatConversation) {
                table.setSelection(item);
            }
            String tooltip = item != null && item.getData() instanceof AIChatConversation conversation
                && event.x >= MARKER_WIDTH && event.x < MARKER_WIDTH + table.getColumn(1).getWidth()
                && (int) item.getData(CAPTION_WIDTH) > table.getColumn(1).getWidth() ? conversation.getCaption() : null;
            if (!Objects.equals(table.getToolTipText(), tooltip)) {
                table.setToolTipText(tooltip);
            }
        });
        table.addListener(SWT.MouseExit, event -> table.setToolTipText(null));
        table.addListener(SWT.Selection, event -> {
            if (table.getSelectionCount() == 1 && table.getSelection()[0].getData() == null) {
                table.deselectAll();
            }
        });
        table.addListener(SWT.MouseUp, event -> {
            TableItem item = table.getItem(new Point(event.x, event.y));
            if (event.button == 1 && item != null && item.getData() instanceof AIChatConversation) {
                table.setSelection(item);
                activateSelection();
            }
        });
        closeOnFocusLost(getShell());
        table.setFocus();
        return area;
    }

    private void resizeColumns() {
        int available = table.getClientArea().width;
        table.getColumn(1).setWidth(Math.max(60, available - MARKER_WIDTH - dateWidth));
        table.getColumn(2).setWidth(dateWidth);
    }

    private void moveSelection(int direction) {
        int index = table.getSelectionIndex();
        if (index < 0) {
            index = direction > 0 ? -1 : table.getItemCount();
        }
        for (index += direction; index >= 0 && index < table.getItemCount(); index += direction) {
            TableItem item = table.getItem(index);
            if (item.getData() instanceof AIChatConversation) {
                table.setSelection(item);
                table.showSelection();
                break;
            }
        }
    }

    private void activateSelection() {
        if (table.getSelectionCount() != 1 || !(table.getSelection()[0].getData() instanceof AIChatConversation conversation)) {
            return;
        }
        close();
        try {
            chat.selectConversation(conversation);
            chat.setFocusOnPrompt();
        } catch (DBException exception) {
            DBWorkbench.getPlatformUI().showError(AIChatMessagesUI.ai_chat_conversation_error, null, exception);
        }
    }

    @NotNull
    @Override
    protected Point getInitialSize() {
        Rectangle monitor = anchor.getMonitor().getClientArea();
        return new Point(Math.min(Math.max(388, anchor.getSize().x), monitor.width - 10),
            Math.min(400, table.getItemCount() * table.getItemHeight() + 10));
    }

    @NotNull
    @Override
    protected Point getInitialLocation(@NotNull Point initialSize) {
        Point origin = anchor.toDisplay(0, anchor.getSize().y);
        Rectangle monitor = anchor.getMonitor().getClientArea();
        int top = origin.y + initialSize.y > monitor.y + monitor.height
            ? anchor.toDisplay(0, 0).y - initialSize.y : origin.y;
        return new Point(Math.max(monitor.x, Math.min(origin.x, monitor.x + monitor.width - initialSize.x)), Math.max(monitor.y, top));
    }
}
