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

import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * SQL editor output viewer
 */
public class SQLEditorOutputViewer extends Composite {

    private final StyledText text;
    private PrintWriter writer;
    private boolean hasNewOutput;

    public SQLEditorOutputViewer(final IWorkbenchPartSite site, Composite parent, int style) {
        super(parent, style);
        setLayout(new FillLayout());

        text = new StyledText(this, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        text.setMargins(5, 5, 5, 5);
        TextEditorUtils.enableHostEditorKeyBindingsSupport(site, text);
        Writer out = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                text.append(String.valueOf(cbuf, off, len));
                if (len > 0) {
                    hasNewOutput = true;
                }
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
            }
        };
        writer = new PrintWriter(out, true);
        createContextMenu(site);
        refreshStyles();
    }

    public StyledText getText() {
        return text;
    }

    void refreshStyles() {
        ITheme currentTheme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
        Font outputFont = currentTheme.getFontRegistry().get(SQLConstants.CONFIG_FONT_OUTPUT);
        if (outputFont != null) {
            this.text.setFont(outputFont);
        }
        this.text.setForeground(currentTheme.getColorRegistry().get(SQLConstants.CONFIG_COLOR_TEXT));
        this.text.setBackground(currentTheme.getColorRegistry().get(SQLConstants.CONFIG_COLOR_BACKGROUND));
    }

    void print(String out)
    {
        text.append(out);
    }

    void println(String out)
    {
        print(out + "\n");
    }

    void scrollToEnd()
    {
        text.setTopIndex(text.getLineCount() - 1);
    }

    public PrintWriter getOutputWriter() {
        return writer;
    }

    public boolean isHasNewOutput() {
        return hasNewOutput;
    }

    void resetNewOutput() {
        hasNewOutput = false;
    }

    private void createContextMenu(IWorkbenchPartSite site)
    {
        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(manager -> {
            StyledTextUtils.fillDefaultStyledTextContextMenu(manager, text);
            manager.add(new Separator());
            manager.add(new Action("Clear") {
                @Override
                public void run() {
                    clearOutput();
                }
            });
        });
        menuMgr.setRemoveAllWhenShown(true);
        text.setMenu(menuMgr.createContextMenu(text));
        text.addDisposeListener(e -> menuMgr.dispose());
    }

    void clearOutput() {
        text.setText("");
    }

}
