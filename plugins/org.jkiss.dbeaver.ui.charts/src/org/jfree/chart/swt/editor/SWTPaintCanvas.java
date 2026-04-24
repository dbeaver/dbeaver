/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jfree.chart.swt.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * A paint canvas.
 */
public class SWTPaintCanvas extends Canvas {
    private Color myColor;
    
    /**
     * Creates a new instance.
     * 
     * @param parent  the parent.
     * @param style  the style.
     * @param color  the color.
     */
    public SWTPaintCanvas(Composite parent, int style, Color color) {
        this(parent, style);
        setColor(color);
    }
    
    /**
     * Creates a new instance.
     * 
     * @param parent  the parent.
     * @param style  the style.
     */
    public SWTPaintCanvas(Composite parent, int style) {
        super(parent, style);
        addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent e) {
                e.gc.setForeground(e.gc.getDevice().getSystemColor(
                        SWT.COLOR_BLACK));
                e.gc.setBackground(SWTPaintCanvas.this.myColor);
                e.gc.fillRectangle(getClientArea());
                e.gc.drawRectangle(getClientArea().x, getClientArea().y, 
                        getClientArea().width - 1, getClientArea().height - 1);
            }
        });
    }
    
    /**
     * Sets the color.
     * 
     * @param color  the color.
     */
    public void setColor(Color color) {
        if (this.myColor != null) {
            this.myColor.dispose();
        }
        //this.myColor = new Color(getDisplay(), color.getRGB());
        this.myColor = color;
    }

    /**
     * Returns the color.
     * 
     * @return The color.
     */
    public Color getColor() {
        return this.myColor;
    }
    
    /**
     * Overridden to do nothing.
     * 
     * @param c  the color.
     */
    public void setBackground(Color c) {
        return;
    }

    /**
     * Overridden to do nothing.
     * 
     * @param c  the color.
     */
    public void setForeground(Color c) {
        return;
    }
    
    /**
     * Frees resources.
     */
    public void dispose() {
        this.myColor.dispose();
    }
}
