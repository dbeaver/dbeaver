/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.model;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.jkiss.dbeaver.erd.model.ERDAttributeVisibility;
import org.jkiss.dbeaver.erd.model.ERDContainer;
import org.jkiss.dbeaver.erd.ui.part.NodePart;
import org.jkiss.dbeaver.model.struct.DBSEntity;

/**
 * ERD object container (diagram)
 */
public interface ERDContainerDecorated extends ERDContainer {

    class NodeVisualInfo {
        public Rectangle initBounds;
        public boolean transparent;
        public Color bgColor;
        public Color fgColor;
        public Font font;
        public int zOrder = 0;
        public int borderWidth = -1;

        public ERDAttributeVisibility attributeVisibility;

        public NodeVisualInfo() {
        }

        public NodeVisualInfo(NodePart part) {
            this.initBounds = part.getBounds();
            IFigure figure = part.getFigure();
            if (figure != null) {
                this.transparent = !figure.isOpaque();
                this.bgColor = figure.getBackgroundColor();
                this.fgColor = figure.getForegroundColor();
                this.font = figure.getFont();
            }
        }
    }

    ERDDecorator getDecorator();

    ERDAttributeVisibility getAttributeVisibility();

    NodeVisualInfo getVisualInfo(DBSEntity entity, boolean create);

}
