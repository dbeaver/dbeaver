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

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.TextFlow;
import org.eclipse.jface.resource.JFaceResources;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.editor.ERDGraphicalViewer;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Figure which represents the whole diagram - the view which corresponds to the
 * Schema model object
 *
 * @author Serge Rider
 */
public class EntityDiagramFigure extends FreeformLayer {

    private DiagramPart part;
    private FlowPage hintFigure;

    public EntityDiagramFigure(DiagramPart diagramPart) {
        this.part = diagramPart;
        hintFigure = new FlowPage();
        hintFigure.setHorizontalAligment(PositionConstants.CENTER);
        TextFlow flow = new TextFlow();
        flow.setFont(JFaceResources.getFont(JFaceResources.HEADER_FONT));
        hintFigure.add(flow);
        //hintFigure.setTextAlignment(PositionConstants.CENTER);
        //hintFigure.setTextPlacement(PositionConstants.MIDDLE);
        add(hintFigure, null);

        //setOpaque(true);
        //setChildrenOrientation(Orientable.HORIZONTAL);
        setBackgroundColor(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_DIAGRAM_BACKGROUND));

        addFigureListener(new FigureListener() {
            @Override
            public void figureMoved(IFigure iFigure) {
                if (hintFigure != null) {
                    Rectangle clientArea = EntityDiagramFigure.this.getClientArea();
                    String message = ((ERDGraphicalViewer) part.getViewer()).getEditor().getErrorMessage();
                    if (!CommonUtils.isEmpty(message)) {
                        TextFlow textFlow = (TextFlow) hintFigure.getChildren().get(0);
                        textFlow.setText(message);
                        Dimension textExtents = FigureUtilities.getTextExtents(message, textFlow.getFont());
                        hintFigure.setLocation(new Point((clientArea.width - textExtents.width) / 2, (clientArea.height - textExtents.height) / 2));
                        hintFigure.setVisible(true);

                        //setConstraint(hintFigure, );
                    } else {
                        hintFigure.setVisible(false);
                    }
                }
                EntityDiagramFigure.this.removeFigureListener(this);
            }
        });
    }

    public DiagramPart getPart() {
        return part;
    }

    @Override
    public void add(IFigure child, Object constraint, int index) {
        if (hintFigure != null && child != hintFigure) {
            remove(hintFigure);
            hintFigure = null;
        }
        super.add(child, constraint, index);
    }
}