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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SharedTextColors implements ISharedTextColors {

    public static final RGB COLOR_WARNING = new RGB(0xFF, 0x63, 0x47);

    /**
     * The display table.
     */
    private final Map<Display, Map<RGB, Color>> fDisplayTable = new HashMap<>();
    private final Map<String, RGB> rgbMap = new HashMap<>();

    public SharedTextColors()
    {
        super();
    }

    @NotNull
    public Color getColor(String rgbString)
    {
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
    public Color getColor(@NotNull RGB rgb)
    {
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
                display.disposeExec(new Runnable() {
                    @Override
                    public void run() {
                        dispose(curDisplay);
                    }
                });
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
    public void dispose()
    {
        for (Map<RGB, Color> rgbColorMap : fDisplayTable.values()) {
            dispose(rgbColorMap);
        }
        fDisplayTable.clear();
    }

    private void dispose(Display display)
    {
        if (fDisplayTable != null) {
            dispose(fDisplayTable.remove(display));
        }
    }

    private void dispose(Map<RGB, Color> colorTable)
    {
        if (colorTable == null)
            return;

        for (Color color : colorTable.values()) {
            color.dispose();
        }

        colorTable.clear();
    }

}
