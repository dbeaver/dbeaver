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
package org.jkiss.dbeaver.erd.ui.notation.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.RelativeBendpoint;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDAttributeVisibility;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotation;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

public class IDEF1XDiagramNotation implements ERDNotation {

    @Override
    public String applyNotation(PolylineConnection conn, ConnectionLayer cLayer, ERDAssociation association, EditPart source,
        EditPart target) {
        final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
        conn.setConnectionRouter(cLayer.getConnectionRouter());
        if (!CommonUtils.isEmpty(association.getInitBends())) {
            List<AbsoluteBendpoint> connBends = new ArrayList<>();
            for (int[] bend : association.getInitBends()) {
                connBends.add(new AbsoluteBendpoint(bend[0], bend[1]));
            }
            conn.setRoutingConstraint(connBends);
        } else if (association.getTargetEntity() != null && association.getTargetEntity() == association.getSourceEntity()) {
            EditPart entityPart = source;
            if (entityPart == null) {
                entityPart = target;
            }
            if (entityPart instanceof GraphicalEditPart
                && (!store.getString(ERDUIConstants.PREF_ROUTING_TYPE).equals(ERDUIConstants.ROUTING_MIKAMI)
                    || ERDAttributeVisibility.isHideAttributeAssociations(store))) {
                final IFigure entityFigure = ((GraphicalEditPart) entityPart).getFigure();
                final Dimension figureSize = entityFigure.getMinimumSize();
                int entityWidth = figureSize.width;
                int entityHeight = figureSize.height;
                List<RelativeBendpoint> bends = new ArrayList<>();
                RelativeBendpoint bpSource = new RelativeBendpoint(conn);
                bpSource.setRelativeDimensions(new Dimension(entityWidth, entityHeight / 2),
                    new Dimension(entityWidth / 2, entityHeight / 2));
                bends.add(bpSource);
                RelativeBendpoint bpTarget = new RelativeBendpoint(conn);
                bpTarget.setRelativeDimensions(new Dimension(-entityWidth, entityHeight / 2), new Dimension(entityWidth, entityHeight));
                bends.add(bpTarget);
                conn.setRoutingConstraint(bends);
            }
        }
        return null;
    }
}
