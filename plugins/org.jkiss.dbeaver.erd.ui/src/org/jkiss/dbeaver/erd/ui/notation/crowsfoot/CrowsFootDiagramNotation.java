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
package org.jkiss.dbeaver.erd.ui.notation.crowsfoot;

import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.model.ERDUtils;
import org.jkiss.dbeaver.erd.ui.notations.ERDAssociationType;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotation;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotationBase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CrowsFootDiagramNotation extends ERDNotationBase implements ERDNotation {

    private static final Log log = Log.getLog(CrowsFootDiagramNotation.class);

    @Override
    public void applyNotationForArrows(PolylineConnection conn, ERDAssociation association, Color bckColor, Color frgColor) {
        DBSEntityConstraintType constraintType = association.getObject().getConstraintType();
        if (constraintType == DBSEntityConstraintType.PRIMARY_KEY) {
            // source 0..1
            createSourceDecorator(conn, bckColor, frgColor, ERDAssociationType.ZERO_OR_ONE, LABEL_0_TO_1);
        } else if (constraintType.isAssociation() &&
            association.getSourceEntity() instanceof ERDEntity &&
            association.getTargetEntity() instanceof ERDEntity) {
            // source - 1..n
            try {
                ERDEntity src = (ERDEntity) association.getSourceEntity();
                DBSEntity entity = src.getObject();
                VoidProgressMonitor monitor = new VoidProgressMonitor();
                Collection<? extends DBSTableIndex> indexes = ((DBSTable) entity).getIndexes(monitor);
                if (!CommonUtils.isEmpty(indexes)) {
                    for (DBSTableIndex index : indexes) {
                        if (DBUtils.isIdentifierIndex(monitor, index)) {
                            List<DBSEntityAttribute> entityIdentifierAttributes = DBUtils.getEntityAttributes(monitor, index);
                            List<DBSEntityAttribute> sourceAttributes = association.getSourceAttributes().stream().map(s -> {
                                return s.getObject();
                            }).collect(Collectors.toList());
                            if (sourceAttributes.containsAll(entityIdentifierAttributes)) {
                                createSourceDecorator(conn, bckColor, frgColor, ERDAssociationType.ONE_ONLY, LABEL_1);
                            } else {
                                createSourceDecorator(conn, bckColor, frgColor, ERDAssociationType.ONE_OR_MANY, LABEL_1_TO_N);
                            }
                        }
                    }
                } else {
                    createSourceDecorator(conn, bckColor, frgColor, ERDAssociationType.ONE_OR_MANY, LABEL_1_TO_N);
                }
                if (ERDUtils.isOptionalAssociation(association)) {
                    // target - 0..1
                    createTargetDecorator(conn, bckColor, frgColor, ERDAssociationType.ZERO_OR_ONE, LABEL_0_TO_1);
                } else {
                    // target - 1
                    createTargetDecorator(conn, bckColor, frgColor, ERDAssociationType.ONE_ONLY, LABEL_1);
                }
            } catch (DBException e) {
                log.error(e.getMessage(), e);
            }
        }
        conn.setLineWidth(1);
        conn.setLineStyle(SWT.LINE_CUSTOM);
    }

    private void createSourceDecorator(PolylineConnection conn, Color bckColor, Color frgColor, ERDAssociationType type, String label) {
        CrowsFootPolylineDecoration sourceDecor;
        sourceDecor = new CrowsFootPolylineDecoration(type);
        sourceDecor.setFill(true);
        sourceDecor.setBackgroundColor(bckColor);
        conn.setSourceDecoration(sourceDecor);
        ConnectionEndpointLocator srcEndpointLocator = new ConnectionEndpointLocator(conn, false);
        srcEndpointLocator.setVDistance(LBL_V_DISTANCE);
        srcEndpointLocator.setUDistance(LBL_U_DISTANCE);
        conn.add(getLabel(label, frgColor), srcEndpointLocator);
    }

    private void createTargetDecorator(PolylineConnection conn, Color bckColor, Color frgColor, ERDAssociationType type, String label) {
        final CrowsFootPolylineDecoration targetDecor = new CrowsFootPolylineDecoration(type);
        targetDecor.setFill(true);
        targetDecor.setBackgroundColor(bckColor);
        ConnectionEndpointLocator trgEndpointLocator = new ConnectionEndpointLocator(conn, true);
        trgEndpointLocator.setVDistance(LBL_V_DISTANCE);
        trgEndpointLocator.setUDistance(LBL_U_DISTANCE);
        conn.add(getLabel(label, frgColor), trgEndpointLocator);
        conn.setTargetDecoration(targetDecor);
    }

    @Override
    public void applyNotationForEntities(PolylineConnection conn, ERDAssociation association, Color bckColor, Color frgColor) {
        // nothing
    }

    @Override
    public double getIndentation() {
        return 30;
    }

}
