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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DBDReferenceUtilsTest {
    @Test
    void compositeAssociationNavigationPreservesSelectedKeyPairs() throws Exception {
        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
        DBDResultSetModel model = mock(DBDResultSetModel.class);
        DBSEntityAssociation association = mock(
            DBSEntityAssociation.class,
            Mockito.withSettings().extraInterfaces(DBSEntityReferrer.class));
        DBSEntityReferrer associationReferrer = (DBSEntityReferrer) association;
        DBSEntityReferrer referencedConstraint = mock(DBSEntityReferrer.class);
        DBSEntity targetEntity = mock(
            DBSEntity.class,
            Mockito.withSettings().extraInterfaces(DBSDataContainer.class));

        DBSEntityAttribute sourceFirst = mock(DBSEntityAttribute.class);
        DBSEntityAttribute sourceSecond = mock(DBSEntityAttribute.class);
        DBSEntityAttribute targetFirst = mock(DBSEntityAttribute.class);
        DBSEntityAttribute targetSecond = mock(DBSEntityAttribute.class);
        DBSEntityAttributeRef sourceFirstRef = attributeRef(sourceFirst);
        DBSEntityAttributeRef sourceSecondRef = attributeRef(sourceSecond);
        DBSEntityAttributeRef targetFirstRef = attributeRef(targetFirst);
        DBSEntityAttributeRef targetSecondRef = attributeRef(targetSecond);

        when(association.getReferencedConstraint()).thenReturn(referencedConstraint);
        when(exposeAttrRefsList(associationReferrer.getAttributeReferences(monitor))).thenReturn(List.of(sourceFirstRef, sourceSecondRef));
        when(exposeAttrRefsList(referencedConstraint.getAttributeReferences(monitor))).thenReturn(List.of(targetFirstRef, targetSecondRef));
        when(referencedConstraint.getParentObject()).thenReturn(targetEntity);

        DBDAttributeBinding firstBinding = bindingFor(sourceFirst);
        DBDAttributeBinding secondBinding = bindingFor(sourceSecond);
        when(model.getAttributes()).thenReturn(new DBDAttributeBinding[]{firstBinding, secondBinding});
        DBDValueRow firstRow = mock(DBDValueRow.class);
        DBDValueRow secondRow = mock(DBDValueRow.class);
        when(model.getCellValue(firstBinding, firstRow)).thenReturn(1);
        when(model.getCellValue(secondBinding, firstRow)).thenReturn(10);
        when(model.getCellValue(firstBinding, secondRow)).thenReturn(2);
        when(model.getCellValue(secondBinding, secondRow)).thenReturn(20);

        DBDDataFilter filter = DBDReferenceUtils.resolveAssociationNavigation(
            monitor, model, association, List.of(firstRow, secondRow)).getTargetFilter();

        assertTrue(filter.isUseDisjunctiveNormalForm());
        DBDAttributeConstraint[] constraints = filter.getConstraints();
        assertEquals(2, constraints.length);
        assertEquals(DBCLogicalOperator.IN, constraints[0].getOperator());
        assertEquals(DBCLogicalOperator.IN, constraints[1].getOperator());
        assertArrayEquals(new Object[]{1, 2}, (Object[]) constraints[0].getValue());
        assertArrayEquals(new Object[]{10, 20}, (Object[]) constraints[1].getValue());
    }

    private static DBSEntityAttributeRef attributeRef(DBSEntityAttribute attribute) {
        DBSEntityAttributeRef ref = mock(DBSEntityAttributeRef.class);
        when(ref.getAttribute()).thenReturn(attribute);
        return ref;
    }

    private static DBDAttributeBinding bindingFor(DBSEntityAttribute attribute) {
        DBDAttributeBinding binding = mock(DBDAttributeBinding.class);
        when(binding.matches(attribute, true)).thenReturn(true);
        when(binding.getValueHandler()).thenReturn(mock(DBDValueHandler.class));
        return binding;
    }

    @Nullable
    private static List<DBSEntityAttributeRef> exposeAttrRefsList(@Nullable List<? extends DBSEntityAttributeRef> list) {
        return list == null ? null : List.copyOf(list);
    }
}
