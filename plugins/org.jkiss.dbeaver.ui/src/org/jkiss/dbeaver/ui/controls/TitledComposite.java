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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.BaseThemeSettings;

/**
 * A composite with a title label above its client.
 * <p>
 * Example usage:
 * <pre>{@code
 * TitledComposite titledComposite = new TitledComposite(parent, SWT.BORDER | SWT.SEPARATOR);
 * titledComposite.setText("Title");
 *
 * Composite client = new Composite(titledComposite, SWT.NONE);
 * // ... add controls to client composite
 *
 * titledComposite.setContent(client);
 * }</pre>
 *
 * <dl>
 * <dt><b>Styles:</b><dd>BORDER, SEPARATOR
 * </dl>
 */
public final class TitledComposite extends Composite {
    final Label title;
    Control client;

    public TitledComposite(@NotNull Composite parent, int style) {
        super(parent, checkStyle(style));
        super.setLayout(new TitledCompositeLayout());

        title = new Label(this, SWT.NONE);

        if (PlatformUI.isWorkbenchRunning()) {
            // If called early, the font might not be available yet
            title.setFont(BaseThemeSettings.instance.baseFontBold);
        }

        if ((getStyle() & SWT.SEPARATOR) == SWT.SEPARATOR) {
            addPaintListener(e -> paintSeparator(e.gc));
        }
    }

    @Override
    public void setLayout(@NotNull Layout layout) {
        checkWidget();
        throw new UnsupportedOperationException("TitledComposite has a fixed layout");
    }

    /**
     * Gets the title text.
     *
     * @return the title text
     */
    @NotNull
    public String getText() {
        return title.getText();
    }

    /**
     * Sets the title text.
     *
     * @param string the title text
     */
    public void setText(@NotNull String string) {
        title.setText(string);
    }

    /**
     * Gets the client control.
     *
     * @return the client control, or {@code null} if none is set
     */
    @Nullable
    public Control getClient() {
        return client;
    }

    /**
     * Sets the client control.
     *
     * @param client the client control, or {@code null} to unset
     */
    public void setClient(@Nullable Control client) {
        this.client = client;
    }

    private static int checkStyle(int style) {
        return style & (SWT.BORDER | SWT.SEPARATOR);
    }

    private void paintSeparator(@NotNull GC gc) {
        Rectangle bounds = getBounds();
        Rectangle label = title.getBounds();

        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        gc.drawLine(
            label.x + label.width + 6,
            label.y + label.height / 2,
            bounds.width,
            label.y + label.height / 2
        );
    }
}
