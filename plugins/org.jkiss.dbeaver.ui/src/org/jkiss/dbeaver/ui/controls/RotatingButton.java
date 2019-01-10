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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public class RotatingButton extends Canvas {
    private int mouse = 0;
    private boolean hit = false;
    private String text = "Button";
    float rotatingAngle = 0f;
    float[] angles = {0, 90, 180, 270};
    int index = 0;

    public RotatingButton(Composite parent, int style) {
        super(parent, style);

        this.addListener(SWT.MouseUp, e -> {
            index++;
            index = index > 3 ? 0 : index;
            Rectangle r = getBounds();

            setBounds(r.x, r.y, r.height, r.width);

            rotatingAngle = angles[index];
            redraw();
        });
        this.addPaintListener(this::paint);
        this.addMouseMoveListener(e -> {
            if (!hit)
                return;
            mouse = 2;
            if (e.x < 0 || e.y < 0 || e.x > getBounds().width
                || e.y > getBounds().height) {
                mouse = 0;
            }
            redraw();
        });
        this.addMouseTrackListener(new MouseTrackAdapter() {
            public void mouseEnter(MouseEvent e) {
                mouse = 1;
                redraw();
            }

            public void mouseExit(MouseEvent e) {
                mouse = 0;
                redraw();
            }
        });
        this.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent e) {
                hit = true;
                mouse = 2;
                redraw();
            }

            public void mouseUp(MouseEvent e) {
                hit = false;
                mouse = 1;
                if (e.x < 0 || e.y < 0 || e.x > getBounds().width
                    || e.y > getBounds().height) {
                    mouse = 0;
                }
                redraw();
                if (mouse == 1)
                    notifyListeners(SWT.Selection, new Event());
            }
        });
        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == '\r' || e.character == ' ') {
                    Event event = new Event();
                    notifyListeners(SWT.Selection, event);
                }
            }
        });
    }

    public void setText(String string) {
        this.text = string;
        redraw();
    }

    public void paint(PaintEvent e) {
        Transform tr = new Transform(e.display);

        Rectangle r = getBounds();
        text = e.gc.stringExtent(text) + "";
        e.gc.setAntialias(SWT.ON);
        Point p = e.gc.stringExtent(text);
        int w = e.width;
        int h = e.height;
        tr.translate(w / 2, h / 2);
        tr.rotate(rotatingAngle);
        e.gc.setTransform(tr);
        e.gc.drawString(text, r.x - (p.x / 3) * 2, r.y - p.y);
    }

}