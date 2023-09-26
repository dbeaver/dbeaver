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
package org.jkiss.dbeaver.erd.ui.notation.crowsfoot;

import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.model.ERDUtils;
import org.jkiss.dbeaver.erd.ui.notations.ERDAssociationType;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotation;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotationBase;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

public class CrowsFootDiagramNotation extends ERDNotationBase implements ERDNotation {
    @Override
    public void applyNotationForArrows(PolylineConnection conn, ERDAssociation association, Color bckColor, Color frgColor) {
        DBSEntityConstraintType constraintType = association.getObject().getConstraintType();
        if (constraintType == DBSEntityConstraintType.PRIMARY_KEY) {
            // source 0..1
            final CrowsFootPolylineDecoration sourceDecor = new CrowsFootPolylineDecoration(ERDAssociationType.ZERO_OR_ONE);
            sourceDecor.setFill(true);
            sourceDecor.setBackgroundColor(bckColor);
            conn.setSourceDecoration(sourceDecor);
        } else if (constraintType.isAssociation() &&
            association.getSourceEntity() instanceof ERDEntity &&
            association.getTargetEntity() instanceof ERDEntity) {
            // source - 1..n
            final CrowsFootPolylineDecoration sourceDecor = new CrowsFootPolylineDecoration(ERDAssociationType.ONE_OR_MANY);
            sourceDecor.setFill(true);
            sourceDecor.setBackgroundColor(bckColor);
            conn.setSourceDecoration(sourceDecor);
            ConnectionEndpointLocator srcEndpointLocator = new ConnectionEndpointLocator(conn, false);
            srcEndpointLocator.setVDistance(LBL_V_DISTANCE);
            srcEndpointLocator.setUDistance(LBL_U_DISTANCE);
            conn.add(getLabel(LABEL_1_TO_N, frgColor), srcEndpointLocator);
            if (ERDUtils.isOptionalAssociation(association)) {
                // target - 0..1
                final CrowsFootPolylineDecoration targetDecor = new CrowsFootPolylineDecoration(ERDAssociationType.ZERO_OR_ONE);
                targetDecor.setFill(true);
                targetDecor.setBackgroundColor(bckColor);
                ConnectionEndpointLocator trgEndpointLocator = new ConnectionEndpointLocator(conn, true);
                trgEndpointLocator.setVDistance(LBL_V_DISTANCE);
                trgEndpointLocator.setUDistance(LBL_U_DISTANCE);
                conn.add(getLabel(LABEL_0_TO_1, frgColor), trgEndpointLocator);
                conn.setTargetDecoration(targetDecor);
            } else {
                // target - 1
                final CrowsFootPolylineDecoration targetDecor = new CrowsFootPolylineDecoration(ERDAssociationType.ONE_ONLY);
                targetDecor.setFill(true);
                targetDecor.setBackgroundColor(bckColor);
                ConnectionEndpointLocator trgEndpointLocator = new ConnectionEndpointLocator(conn, true);
                trgEndpointLocator.setVDistance(LBL_V_DISTANCE);
                trgEndpointLocator.setUDistance(LBL_U_DISTANCE);
                conn.add(getLabel(LABEL_1, frgColor), trgEndpointLocator);
                conn.setTargetDecoration(targetDecor);
            }
        }
        conn.setLineWidth(1);
        conn.setLineStyle(SWT.LINE_CUSTOM);
    }

    @Override
    public void applyNotationForEntities(PolylineConnection conn, ERDAssociation association, Color bckColor, Color frgColor) {
        // nothing
    }

}
