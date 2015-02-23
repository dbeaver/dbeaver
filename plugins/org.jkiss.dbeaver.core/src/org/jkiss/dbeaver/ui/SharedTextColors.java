/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SharedTextColors implements ISharedTextColors {

    public static final RGB COLOR_WARNING = new RGB(0xFF, 0x63, 0x47);

    /**
     * The display table.
     */
    private Map<Display, Map<RGB, Color>> fDisplayTable;

    public SharedTextColors()
    {
        super();
    }

    @Nullable
    @Override
    public Color getColor(RGB rgb)
    {
        if (rgb == null)
            return null;

        if (fDisplayTable == null)
            fDisplayTable = new HashMap<Display, Map<RGB, Color>>(2);

        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        final Display curDisplay = display;

        Map<RGB, Color> colorTable = fDisplayTable.get(display);
        if (colorTable == null) {
            colorTable = new HashMap<RGB, Color>(10);
            fDisplayTable.put(curDisplay, colorTable);
            display.disposeExec(new Runnable() {
                @Override
                public void run()
                {
                    dispose(curDisplay);
                }
            });
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
        if (fDisplayTable == null)
            return;

        for (Map<RGB, Color> rgbColorMap : fDisplayTable.values()) {
            dispose(rgbColorMap);
        }
        fDisplayTable = null;
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
