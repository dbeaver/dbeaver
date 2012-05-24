/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import java.util.HashMap;
import java.util.Map;

public class SharedTextColors implements ISharedTextColors {

    /**
     * The display table.
     */
    private Map<Display, Map<RGB, Color>> fDisplayTable;

    public SharedTextColors()
    {
        super();
    }

    @Override
    public Color getColor(RGB rgb)
    {
        if (rgb == null)
            return null;

        if (fDisplayTable == null)
            fDisplayTable = new HashMap<Display, Map<RGB, Color>>(2);

        final Display display = Display.getCurrent();

        Map<RGB, Color> colorTable = fDisplayTable.get(display);
        if (colorTable == null) {
            colorTable = new HashMap<RGB, Color>(10);
            fDisplayTable.put(display, colorTable);
            display.disposeExec(new Runnable() {
                @Override
                public void run()
                {
                    dispose(display);
                }
            });
        }

        Color color = colorTable.get(rgb);
        if (color == null) {
            color = new Color(display, rgb);
            colorTable.put(rgb, color);
        }

        return color;
    }

    @Override
    public void dispose()
    {
        if (fDisplayTable == null)
            return;

        for (Map<RGB, Color> rgbColorMap : fDisplayTable.values())
            dispose(rgbColorMap);
        fDisplayTable = null;
    }

    private void dispose(Display display)
    {
        if (fDisplayTable != null)
            dispose(fDisplayTable.remove(display));
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
