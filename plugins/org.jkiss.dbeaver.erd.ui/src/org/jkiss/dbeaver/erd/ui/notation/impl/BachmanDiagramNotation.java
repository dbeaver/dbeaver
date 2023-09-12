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

import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.model.ERDUtils;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotation;
import org.jkiss.dbeaver.erd.ui.part.AssociationPart.CircleDecoration;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

public class BachmanDiagramNotation implements ERDNotation {

    private static final String LABEL_0_TO_1 = "0..1";
    private static final String LABEL_1 = "1";
    private static final String LABEL_1_TO_N = "1..n";
    private static final int CIRCLE_RADIUS = 5;

    @Override
    public void applyNotation(PolylineConnection conn, ERDAssociation association, Color bckColor, Color frgColor) {
        DBSEntityConstraintType constraintType = association.getObject().getConstraintType();
        if (constraintType == DBSEntityConstraintType.PRIMARY_KEY) {
            // source 0..1
            final CircleDecoration sourceDecor = new CircleDecoration();
            sourceDecor.setRadius(CIRCLE_RADIUS);
            sourceDecor.setFill(true);
            sourceDecor.setBackgroundColor(frgColor);
            conn.setSourceDecoration(sourceDecor);

        } else if (constraintType.isAssociation() &&
            association.getSourceEntity() instanceof ERDEntity &&
            association.getTargetEntity() instanceof ERDEntity) {
            // source - 1..n
            final PolygonDecoration sourceDecor = new PolygonDecoration();
            sourceDecor.setTemplate(PolygonDecoration.TRIANGLE_TIP);

            sourceDecor.setScale(15, 5);
            sourceDecor.setFill(true);
            sourceDecor.setBackgroundColor(frgColor);
            ConnectionEndpointLocator srcEndpointLocator = new ConnectionEndpointLocator(conn, false);
            srcEndpointLocator.setVDistance(15);
            conn.add(new Label(LABEL_1_TO_N), srcEndpointLocator);
            conn.setSourceDecoration(sourceDecor);

            if (ERDUtils.isOptionalAssociation(association)) {
                // target - 0..1
                final CircleDecoration targetDecor = new CircleDecoration();
                targetDecor.setRadius(CIRCLE_RADIUS);
                targetDecor.setFill(true);
                targetDecor.setBackgroundColor(bckColor);

                ConnectionEndpointLocator trgEndpointLocator = new ConnectionEndpointLocator(conn, true);
                trgEndpointLocator.setVDistance(15);
                conn.add(new Label(LABEL_0_TO_1), trgEndpointLocator);
                conn.setTargetDecoration(targetDecor);

            } else {
                // target - 1
                final CircleDecoration targetDecor = new CircleDecoration();
                targetDecor.setRadius(CIRCLE_RADIUS);
                targetDecor.setFill(true);
                targetDecor.setBackgroundColor(frgColor);
                ConnectionEndpointLocator trgEndpointLocator = new ConnectionEndpointLocator(conn, true);
                trgEndpointLocator.setVDistance(15);
                conn.add(new Label(LABEL_1), trgEndpointLocator);
                conn.setTargetDecoration(targetDecor);
            }
        }
        conn.setLineWidth(1);
        conn.setLineStyle(SWT.LINE_CUSTOM);
    }

}
