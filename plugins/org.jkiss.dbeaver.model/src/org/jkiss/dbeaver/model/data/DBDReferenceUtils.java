/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for resolving result set association navigation
 */
public final class DBDReferenceUtils {

    private static final Log log = Log.getLog(DBDReferenceUtils.class);

    private DBDReferenceUtils() {
    }

    @NotNull
    public static DBDReferenceNavigation resolveAssociationNavigation(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBDResultSetModel sourceModel,
        @NotNull DBSEntityAssociation association,
        @NotNull List<? extends DBDValueRow> rows
    ) throws DBException {
        if (rows.isEmpty()) {
            throw new DBException("Can't navigate association without selected rows");
        }
        DBSEntityConstraint refConstraint = association.getReferencedConstraint();
        if (refConstraint == null) {
            throw new DBException("Broken association (referenced constraint missing)");
        }
        if (!(association instanceof DBSEntityReferrer)) {
            throw new DBException("Association [" + association + "] is not a referrer");
        }
        if (!(refConstraint instanceof DBSEntityReferrer)) {
            throw new DBException("Referenced constraint [" + refConstraint + "] is not a referrer");
        }
        DBSEntity targetEntity = refConstraint.getParentObject();
        targetEntity = DBVUtils.getRealEntity(monitor, targetEntity);
        if (!(targetEntity instanceof DBSDataContainer)) {
            throw new DBException(
                "Entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] is not a data container");
        }

        // make constraints
        List<DBDAttributeConstraint> constraints = new ArrayList<>();

        // Set conditions
        List<? extends DBSEntityAttributeRef> ownAttrs = CommonUtils.safeList(((DBSEntityReferrer) association).getAttributeReferences(
            monitor));
        List<? extends DBSEntityAttributeRef> refAttrs = CommonUtils.safeList(((DBSEntityReferrer) refConstraint).getAttributeReferences(
            monitor));
        if (ownAttrs.size() != refAttrs.size()) {
            throw new DBException(
                "Entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] association [" + association.getName() +
                    "] columns differs from referenced constraint [" + refConstraint.getName() + "] (" + ownAttrs.size() + "<>"
                    + refAttrs.size() + ")");
        }
        // Add association constraints
        for (int i = 0; i < ownAttrs.size(); i++) {
            DBSEntityAttributeRef ownAttr = ownAttrs.get(i);
            DBSEntityAttributeRef refAttr = refAttrs.get(i);
            DBDAttributeBinding ownBinding = DBUtils.findBinding(sourceModel.getAttributes(), ownAttr.getAttribute());
            if (ownBinding == null) {
                throw new DBException("Attribute " + ownAttr.getAttribute() + " is missing in result set");
            }

            DBSEntityAttribute attribute = refAttr.getAttribute();
            if (attribute != null) {
                DBDAttributeConstraint constraint = new DBDAttributeConstraint(attribute, DBDAttributeConstraint.NULL_VISUAL_POSITION);
                constraint.setVisible(true);
                constraints.add(constraint);
                createFilterConstraint(sourceModel, rows, ownBinding, constraint);
            }

        }
        return new DBDReferenceNavigation(targetEntity, new DBDDataFilter(constraints));
    }

    @NotNull
    public static DBDReferenceNavigation resolveReferenceNavigation(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBDResultSetModel sourceModel,
        @NotNull DBSEntityAssociation association,
        @NotNull List<? extends DBDValueRow> rows
    ) throws DBException {
        DBSEntity targetEntity = association.getParentObject();
        //DBSDataContainer dataContainer = DBUtils.getAdapter(DBSDataContainer.class, targetEntity);
        targetEntity = DBVUtils.getRealEntity(monitor, targetEntity);
        if (!(targetEntity instanceof DBSDataContainer)) {
            throw new DBException("Referencing entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] is not a data container");
        }

        // make constraints
        List<DBDAttributeConstraint> constraints = new ArrayList<>();

        // Set conditions
        DBSEntityConstraint refConstraint = association.getReferencedConstraint();
        if (refConstraint == null) {
            throw new DBException("Can't obtain association '" + DBUtils.getQuotedIdentifier(association) + "' target constraint (table " +
                (association.getAssociatedEntity() == null ? "???" : DBUtils.getQuotedIdentifier(association.getAssociatedEntity())) + ")");
        }
        List<? extends DBSEntityAttributeRef> ownAttrs = CommonUtils.safeList(((DBSEntityReferrer) association).getAttributeReferences(monitor));
        List<? extends DBSEntityAttributeRef> refAttrs = CommonUtils.safeList(((DBSEntityReferrer) refConstraint).getAttributeReferences(monitor));
        if (ownAttrs.size() != refAttrs.size()) {
            throw new DBException(
                "Entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] association [" + association.getName() +
                    "] columns differ from referenced constraint [" + refConstraint.getName() + "] (" + ownAttrs.size() + "<>" + refAttrs.size() + ")");
        }
        if (ownAttrs.isEmpty()) {
            throw new DBException("Association '" + DBUtils.getQuotedIdentifier(association) + "' has empty column list");
        }
        // Add association constraints
        for (int i = 0; i < refAttrs.size(); i++) {
            DBSEntityAttributeRef refAttr = refAttrs.get(i);

            DBDAttributeBinding attrBinding = DBUtils.findBinding(sourceModel.getAttributes(), refAttr.getAttribute());
            if (attrBinding == null) {
                log.error("Can't find attribute binding for ref attribute '" + refAttr.getAttribute().getName() + "'");
            } else {
                // Constrain use corresponding own attr
                DBSEntityAttributeRef ownAttr = ownAttrs.get(i);
                DBDAttributeConstraint constraint = new DBDAttributeConstraint(ownAttr.getAttribute(), DBDAttributeConstraint.NULL_VISUAL_POSITION);
                constraint.setVisible(true);
                constraints.add(constraint);

                createFilterConstraint(sourceModel, rows, attrBinding, constraint);

            }
        }
        return new DBDReferenceNavigation(targetEntity, new DBDDataFilter(constraints));
    }

    private static void createFilterConstraint(
        @NotNull DBDResultSetModel sourceModel,
        @NotNull List<? extends DBDValueRow> rows,
        @NotNull DBDAttributeBinding attrBinding,
        @NotNull DBDAttributeConstraint constraint
    ) throws DBException {
        if (rows.size() == 1) {
            Object keyValue = sourceModel.getCellValue(attrBinding, rows.getFirst());
            constraint.setOperator(DBCLogicalOperator.EQUALS);
            constraint.setValue(keyValue);
        } else {
            Object[] keyValues = new Object[rows.size()];
            for (int k = 0; k < rows.size(); k++) {
                keyValues[k] = sourceModel.getCellValue(attrBinding, rows.get(k));
            }
            DBCLogicalOperator[] supportedOperators = attrBinding.getValueHandler().getSupportedOperators(attrBinding);
            if (ArrayUtils.contains(supportedOperators, DBCLogicalOperator.IN)) {
                constraint.setOperator(DBCLogicalOperator.IN);
            } else {
                constraint.setOperator(DBCLogicalOperator.EQUALS);
            }
            constraint.setValue(keyValues);
        }
    }
}
