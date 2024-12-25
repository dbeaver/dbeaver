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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.erd.ui.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.ui.editor.ERDThemeSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Figure used to hold the column labels
 *
 * @author Serge Rider
 */
public class AttributeListFigure extends Figure {

    public AttributeListFigure(ERDEntity entity, boolean key) {
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 3;
        layout.marginWidth = 5;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
/*
        FlowLayout layout = new FlowLayout(false);
		layout.setMinorAlignment(FlowLayout.ALIGN_TOPLEFT);
		layout.setStretchMinorAxis(true);
		layout.setMinorSpacing(2);
*/
        setLayoutManager(layout);
        setBorder(new ColumnFigureBorder());
        setBackgroundColor(ERDThemeSettings.instance.attrBackground);
        setForegroundColor(ERDThemeSettings.instance.attrForeground);

        setOpaque(true);
    }

    public List<AttributeItemFigure> getAttributes() {
        List<AttributeItemFigure> result = new ArrayList<>();
        for (Object child : getChildren()) {
            if (child instanceof AttributeItemFigure) {
                result.add((AttributeItemFigure) child);
            }
        }
        return result;
    }

    class ColumnFigureBorder extends AbstractBorder {

        @Override
        public Insets getInsets(IFigure figure) {
            return new Insets(0, -2, -2, -2);
        }

        @Override
        public void paint(IFigure figure, Graphics graphics, Insets insets) {
            graphics.setForegroundColor(((EntityFigure) getParent()).getBorderColor());
            Rectangle rect = getPaintRectangle(figure, insets);
            graphics.setLineWidth(2);
            graphics.drawLine(rect.getTopLeft(), tempRect.getTopRight());
        }
    }
}