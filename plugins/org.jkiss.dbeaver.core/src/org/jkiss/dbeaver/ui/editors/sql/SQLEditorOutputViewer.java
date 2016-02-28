/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * SQL content type describer
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
        UIUtils.enableHostEditorKeyBindingsSupport(site, text);
        Writer out = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
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
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                UIUtils.fillDefaultStyledTextContextMenu(manager, text);
                manager.add(new Separator());
                manager.add(new Action("Clear") {
                    @Override
                    public void run() {
                        text.setText("");
                    }
                });
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        text.setMenu(menuMgr.createContextMenu(text));
    }

}
