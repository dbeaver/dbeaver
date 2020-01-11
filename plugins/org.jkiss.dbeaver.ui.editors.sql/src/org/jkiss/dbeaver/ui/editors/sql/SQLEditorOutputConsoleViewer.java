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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class SQLEditorOutputConsoleViewer extends TextConsoleViewer {

    private MessageConsole console;
    private boolean hasNewOutput;
    private PrintWriter writer;

    public SQLEditorOutputConsoleViewer(IWorkbenchPartSite site, CTabFolder resultTabs, int styles) {
        this(site, resultTabs, new MessageConsole("sql-output", null));
    }

    private SQLEditorOutputConsoleViewer(IWorkbenchPartSite site, CTabFolder resultTabs, MessageConsole console) {
        super(resultTabs, console);
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

    public boolean isDisposed() {
        return this.getControl().isDisposed();
    }

    public PrintWriter getOutputWriter() {
        return writer;
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
        ITheme currentTheme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
        Font outputFont = currentTheme.getFontRegistry().get(SQLConstants.CONFIG_FONT_OUTPUT);
        if (outputFont != null) {
            getTextWidget().setFont(outputFont);
        }
        getTextWidget().setForeground(UIStyles.getDefaultTextForeground());
        getTextWidget().setBackground(UIStyles.getDefaultTextBackground());
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
            manager.add(new Action("Clear") {
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
