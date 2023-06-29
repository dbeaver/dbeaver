/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.swt.graphics.Color;

public class RoundedLineBorder extends LineBorder {
    protected int arcLength;
    protected int lineStyle = 1;
    private int margin = 0;

    public RoundedLineBorder(Color c, int width, int arcLength) {
        super(c, width);
        this.arcLength = arcLength;
    }

    public RoundedLineBorder(int width, int arcLength) {
        super(width);
        this.arcLength = arcLength;
    }

    public RoundedLineBorder(Color c, int width, int arcLength, int lineStyle) {
        super(c, width);
        this.arcLength = arcLength;
        this.lineStyle = lineStyle;
    }

    public RoundedLineBorder(int width, int arcLength, int lineStyle) {
        super(width);
        this.arcLength = arcLength;
        this.lineStyle = lineStyle;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    @Override
    public Insets getInsets(IFigure figure) {
        return margin <= 0 ? super.getInsets(figure) : new Insets(margin);
    }

    public void paint(IFigure figure, Graphics graphics, Insets insets) {
        int rlbWidth = this.getWidth();
        tempRect.setBounds(getPaintRectangle(figure, insets));
        if (rlbWidth % 2 == 1) {
            tempRect.width--;
            tempRect.height--;
        }

        tempRect.shrink(rlbWidth / 2, rlbWidth / 2);
        graphics.setLineWidth(rlbWidth);
        graphics.setLineStyle(this.lineStyle);
        if (this.getColor() != null) {
            graphics.setForegroundColor(this.getColor());
        }

        graphics.drawRoundRectangle(tempRect, this.arcLength, this.arcLength);
    }
}
