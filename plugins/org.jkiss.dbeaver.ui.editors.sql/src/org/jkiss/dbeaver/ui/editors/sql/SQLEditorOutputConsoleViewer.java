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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.TextConsoleViewer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class SQLEditorOutputConsoleViewer extends TextConsoleViewer {

    private MessageConsole console;
    private boolean hasNewOutput;
    private PrintWriter writer;

    public SQLEditorOutputConsoleViewer(IWorkbenchPartSite site, Composite parent, int styles) {
        this(site, parent, new MessageConsole("sql-output", null));
    }

    protected SQLEditorOutputConsoleViewer(IWorkbenchPartSite site, Composite parent, MessageConsole console) {
        super(parent, console);
        this.console = console;
        this.getText().setMargins(5, 5, 5, 5);
        this.console.setWaterMarks(1024*1024*10, 1024*1024*20);

        TextEditorUtils.enableHostEditorKeyBindingsSupport(site, this.getText());

        setEditable(false);

        createContextMenu();
        refreshStyles();

        OutputStream consoleOutputStream = console.newOutputStream();
        OutputStream out = new OutputStream() {
            @Override
            public void write(final byte[] buf, final int off, final int len) throws IOException {
                consoleOutputStream.write(buf, off, len);
                hasNewOutput = true;
            }
            @Override
            public void flush() throws IOException {
                consoleOutputStream.flush();
            }
            @Override
            public void close() throws IOException {
                consoleOutputStream.flush();
            }
            @Override
            public void write(int b) throws IOException {
                consoleOutputStream.write(b);
            }
        };
        writer = new PrintWriter(out, true);
    }

    public void dispose() {
        this.getControl().dispose();
    }

    public boolean isDisposed() {
        return this.getControl().isDisposed();
    }

    public PrintWriter getOutputWriter() {
        return writer;
    }

    @NotNull
    public MessageConsole getConsole() {
        return console;
    }

    public void scrollToEnd() {
        revealEndOfDocument();
    }

    public boolean isVisible() {
        return getControl().getVisible();
    }

    public void resetNewOutput() {
        hasNewOutput = false;
    }

    public void clearOutput() {
        console.clearConsole();
    }

    public void refreshStyles() {
        Font outputFont = BaseThemeSettings.instance.monospaceFont;
        if (outputFont != null) {
            getTextWidget().setFont(outputFont);
        }
        if (UIStyles.isDarkHighContrastTheme()) {
            getTextWidget().setForeground(UIUtils.COLOR_WHITE);
            getTextWidget().setBackground(UIStyles.getDefaultWidgetBackground());
        } else {
            getTextWidget().setForeground(UIStyles.getDefaultTextForeground());
            getTextWidget().setBackground(UIStyles.getDefaultTextBackground());
        }
    }

    public StyledText getText() {
        return getTextWidget();
    }

    public boolean isHasNewOutput() {
        return hasNewOutput;
    }

    private void createContextMenu() {
        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(manager -> {
            StyledTextUtils.fillDefaultStyledTextContextMenu(manager, getTextWidget());
            manager.add(new Separator());
            manager.add(new Action(SQLEditorMessages.sql_editor_action_clear) {
                @Override
                public void run() {
                    clearOutput();
                }
            });
        });
        menuMgr.setRemoveAllWhenShown(true);
        getTextWidget().setMenu(menuMgr.createContextMenu(getTextWidget()));
        getTextWidget().addDisposeListener(e -> menuMgr.dispose());
    }

}
