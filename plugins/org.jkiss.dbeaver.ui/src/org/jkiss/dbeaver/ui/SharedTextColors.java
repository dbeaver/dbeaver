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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SharedTextColors implements ISharedTextColors {

    public static final RGB COLOR_WARNING = new RGB(0xFF, 0x63, 0x47);

    /**
     * The display table.
     */
    private final Map<Display, Map<RGB, Color>> fDisplayTable = new HashMap<>();
    private final Map<String, RGB> rgbMap = new HashMap<>();

    public SharedTextColors() {
        super();
    }

    @NotNull
    public Color getColor(String rgbString) {
        RGB rgb;
        synchronized (rgbMap) {
            rgb = rgbMap.get(rgbString);
            if (rgb == null) {
                rgb = StringConverter.asRGB(rgbString);
                rgbMap.put(rgbString, rgb);
            }
        }
        return getColor(rgb);
    }

    @NotNull
    @Override
    public Color getColor(@NotNull RGB rgb) {
        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        final Display curDisplay = display;

        Map<RGB, Color> colorTable;
        synchronized (fDisplayTable) {
            colorTable = fDisplayTable.get(display);
            if (colorTable == null) {
                colorTable = new HashMap<>(10);
                fDisplayTable.put(curDisplay, colorTable);
                display.disposeExec(() -> dispose(curDisplay));
            }
        }

        Color color = colorTable.get(rgb);
        if (color == null) {
            color = new Color(curDisplay, rgb);
            colorTable.put(rgb, color);
        }

        return color;
    }

    @Override
    public void dispose() {
        for (Map<RGB, Color> rgbColorMap : fDisplayTable.values()) {
            dispose(rgbColorMap);
        }
        fDisplayTable.clear();
    }

    private void dispose(Display display) {
        dispose(fDisplayTable.remove(display));
    }

    private void dispose(Map<RGB, Color> colorTable) {
        if (colorTable == null)
            return;

        for (Color color : colorTable.values()) {
            color.dispose();
        }

        colorTable.clear();
    }

}
