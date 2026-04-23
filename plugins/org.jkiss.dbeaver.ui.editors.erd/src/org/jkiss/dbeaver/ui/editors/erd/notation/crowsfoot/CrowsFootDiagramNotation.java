/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.erd.notation.crowsfoot;

import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.erd.ERDAssociation;
import org.jkiss.dbeaver.model.erd.ERDEntity;
import org.jkiss.dbeaver.model.erd.ERDEntityAttribute;
import org.jkiss.dbeaver.model.erd.ERDUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.erd.notations.ERDAssociationType;
import org.jkiss.dbeaver.ui.editors.erd.notations.ERDNotation;
import org.jkiss.dbeaver.ui.editors.erd.notations.ERDNotationBase;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

public class CrowsFootDiagramNotation extends ERDNotationBase implements ERDNotation {

    private static final Log log = Log.getLog(CrowsFootDiagramNotation.class);

    @Override
    public void applyNotationForArrows(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PolylineConnection conn,
        @NotNull ERDAssociation association,
        @NotNull Color bckColor,
        @NotNull Color frgColor
    ) {
        DBSEntityConstraintType constraintType = association.getObject().getConstraintType();
        if (constraintType == DBSEntityConstraintType.PRIMARY_KEY) {
            // source 0..1
            createSourceDecorator(conn, bckColor, frgColor, ERDAssociationType.ZERO_OR_ONE, LABEL_0_TO_1);
        } else if (constraintType.isAssociation() &&
            association.getSourceEntity() instanceof ERDEntity src &&
            association.getTargetEntity() instanceof ERDEntity trg) {
            // source - 1..n
            try {
                DBSEntity entity = src.getObject();
                Collection<? extends DBSTableIndex> indexes = ((DBSTable) entity).getIndexes(monitor);
                if (!CommonUtils.isEmpty(indexes)) {
                    // get index for require source attributes
                    List<ERDEntityAttribute> erdSourceAttributes = association.getSourceAttributes();
                    List<DBSEntityAttribute> attributes = erdSourceAttributes.stream()
                        .map(ERDEntityAttribute::getObject)
                        .toList();
                    if (DBUtils.isUniqueIndexForAttributes(monitor, attributes, entity)) {
                        createSourceDecorator(conn, bckColor, frgColor, ERDAssociationType.ONE_ONLY, LABEL_1);
                    } else {
                        createSourceDecorator(conn, bckColor, frgColor, ERDAssociationType.ONE_OR_MANY, LABEL_1_TO_N);
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
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                /* Clean up whatever needs to be handled before interrupting */
                Thread.currentThread().interrupt();
            }
        }
        conn.setLineWidth(1);
        conn.setLineStyle(SWT.LINE_CUSTOM);

        if (association.getObject() instanceof DBSTableForeignKey) {
            DBSForeignKeyModifyRule deleteRule = ((DBSTableForeignKey) association.getObject()).getDeleteRule();
            /*
            ColorRegistry colorRegistry = UIUtils.getCurrentTheme().getColorRegistry();
            Color fgColor = colorRegistry.get(getDeleteRuleColorKey(deleteRule));

            conn.setForegroundColor(fgColor);
             */

            boolean darkTheme = UIUtils.getCurrentTheme().getId().toLowerCase().contains("dark");
            conn.setForegroundColor(switch (deleteRule.getId()) {
                case DBSForeignKeyModifyRule.ID.NO_ACTION ->   darkTheme ? new Color(176, 122, 255) : new Color(126,  87, 194);
                case DBSForeignKeyModifyRule.ID.CASCADE ->     darkTheme ? new Color(255,  95,  95) : new Color(198,  40,  40);
                case DBSForeignKeyModifyRule.ID.SET_NULL ->    darkTheme ? new Color( 92, 201, 110) : new Color( 46, 125,  50);
                case DBSForeignKeyModifyRule.ID.SET_DEFAULT -> darkTheme ? new Color(100, 181, 246) : new Color( 21, 101, 192);
                case DBSForeignKeyModifyRule.ID.RESTRICT ->    darkTheme ? new Color(255, 179,  71) : new Color(239, 108,   0);
                case DBSForeignKeyModifyRule.ID.UNKNOWN ->     darkTheme ? new Color(150, 150, 150) : new Color(120, 120, 120);
            });
        }
    }

    /*
    private String getDeleteRuleColorKey(DBSForeignKeyModifyRule rule) {
        return switch (rule.getId()) {
            case "UNKNOWN" -> "dbeaver.erd.deleteRule.unknown";
            case "NO_ACTION" -> "dbeaver.erd.deleteRule.noAction";
            case "CASCADE" -> "dbeaver.erd.deleteRule.cascade";
            case "SET_NULL" -> "dbeaver.erd.deleteRule.setNull";
            case "SET_DEFAULT" -> "dbeaver.erd.deleteRule.setDefault";
            case "RESTRICT" -> "dbeaver.erd.deleteRule.restrict";
            default -> "dbeaver.erd.deleteRule.undefined";
        };
    }
     */

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
    public void applyNotationForEntities(
        @NotNull PolylineConnection conn,
        @NotNull ERDAssociation association,
        @NotNull Color bckColor,
        @NotNull Color frgColor
    ) {
        // nothing
    }

    @Override
    public double getIndentation() {
        return 30;
    }

}
