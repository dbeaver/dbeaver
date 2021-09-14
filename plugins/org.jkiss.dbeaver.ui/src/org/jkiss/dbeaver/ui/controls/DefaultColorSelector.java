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

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;

public class DefaultColorSelector extends ColorSelector {
    private RGB defaultColorValue;

    public DefaultColorSelector(@NotNull Composite parent) {
        super(parent);

        getButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                if ((e.stateMask & SWT.BUTTON3) > 0) {
                    setColorValue(getDefaultColorValue());
                    fireColorUpdateEvent(getColorValue(), getDefaultColorValue());
                }
            }
        });

        getButton().setToolTipText(UIMessages.control_default_color_selector_reset_default_tip);
    }

    @Override
    protected void updateColorImage() {
        if (getColorValue() == null) {
            return;
        }

        super.updateColorImage();

        if (getColorValue() == getDefaultColorValue()) {
            final Button button = getButton();
            final Image image = button.getImage();
            final Rectangle bounds = image.getBounds();
            final GC gc = new GC(image);

            final Color color = UIUtils.getSharedColor(getColorValue());
            final Color contrastColor = UIUtils.getContrastColor(color);
            final Color blendedColor = UIUtils.getSharedColor(UIUtils.blend(color.getRGB(), contrastColor.getRGB(), 50));

            // Draw overlay and cross with contrast color
            gc.setForeground(blendedColor);
            gc.drawRectangle(1, 1, bounds.width - 3, bounds.height - 3);
            gc.drawLine(2, 2, bounds.width - 3, bounds.height - 3);
            gc.drawLine(bounds.width - 3, 2, 2, bounds.height - 3);

            gc.dispose();
            button.setImage(image);
        }
    }

    @NotNull
    public RGB getDefaultColorValue() {
        return defaultColorValue;
    }

    public void setDefaultColorValue(@NotNull RGB defaultColorValue) {
        this.defaultColorValue = defaultColorValue;
        updateColorImage();
    }

    /* Copied from ColorSelector#open() */
    private void fireColorUpdateEvent(@NotNull RGB oldColor, @NotNull RGB newColor) {
        final Object[] listeners = getListeners();
        if (listeners.length > 0) {
            final PropertyChangeEvent event = new PropertyChangeEvent(this, PROP_COLORCHANGE, oldColor, newColor);
            for (Object listener : listeners) {
                ((IPropertyChangeListener) listener).propertyChange(event);
            }
        }
    }
}
