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
package org.jkiss.dbeaver.erd.ui.layout;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.Animation;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.layout.algorithm.direct.DirectedGraphLayoutVisitor;
import org.jkiss.dbeaver.erd.ui.part.DiagramPart;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;


/**
 * Uses the DirectedGraphLayoutVisitor to automatically lay out figures on diagram
 * @author Serge Rider
 */
public class GraphLayoutAuto extends AbstractLayout {

    private DiagramPart diagram;

    public GraphLayoutAuto(DiagramPart diagram) {
        this.diagram = diagram;
    }

    @Override
    protected Dimension calculatePreferredSize(IFigure container, int widthHint, int heightHint) {
        container.validate();
        List<? extends IFigure> children = container.getChildren();
        Rectangle result = new Rectangle().setLocation(container.getClientArea().getLocation());
        for (IFigure child : children) {
            result.union(child.getBounds());
        }
        result.resize(container.getInsets().getWidth(), container.getInsets().getHeight());
        return result.getSize();
    }

    @Override
    public void layout(IFigure container) {
        DBRProgressMonitor monitor = diagram.getDiagram().getMonitor();
        monitor.subTask(ERDUIMessages.erd_job_layout_diagram);
        UIUtils.syncExec(() -> {
            new DirectedGraphLayoutVisitor(diagram.getDiagram().getDecorator()).layoutDiagram(diagram);
            diagram.setTableModelBounds();
        });
    }
}