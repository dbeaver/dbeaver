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
    /**
     * Test reference panel result when multiple rows selected in a table with a composite foreign key
     */
    @Test
    void compositeAssociationNavigationPreservesSelectedKeyPairs() throws Exception {
        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
        DBDResultSetModel model = mock(DBDResultSetModel.class);
        DBSEntityAttribute sourceTripId = mock(DBSEntityAttribute.class);
        DBSEntityAttribute sourceSegmentId = mock(DBSEntityAttribute.class);
        DBSEntityAttribute targetTripId = mock(DBSEntityAttribute.class);
        DBSEntityAttribute targetSegmentId = mock(DBSEntityAttribute.class);
        DBSEntityAssociation association = compositeAssociation(
            monitor,
            List.of(sourceTripId, sourceSegmentId),
            List.of(targetTripId, targetSegmentId)
        );

        DBDAttributeBinding tripIdBinding = bindingFor(sourceTripId);
        DBDAttributeBinding segmentIdBinding = bindingFor(sourceSegmentId);
        when(model.getAttributes()).thenReturn(new DBDAttributeBinding[]{tripIdBinding, segmentIdBinding});

        DBDValueRow tripOneSegmentTen = mock(DBDValueRow.class);
        DBDValueRow tripTwoSegmentTwenty = mock(DBDValueRow.class);
        when(model.getCellValue(tripIdBinding, tripOneSegmentTen)).thenReturn(1);
        when(model.getCellValue(segmentIdBinding, tripOneSegmentTen)).thenReturn(10);
        when(model.getCellValue(tripIdBinding, tripTwoSegmentTwenty)).thenReturn(2);
        when(model.getCellValue(segmentIdBinding, tripTwoSegmentTwenty)).thenReturn(20);

        DBDDataFilter filter = DBDReferenceUtils.resolveAssociationNavigation(
            monitor,
            model,
            association,
            List.of(tripOneSegmentTen, tripTwoSegmentTwenty)
        ).getTargetFilter();

        assertTrue(filter.isUseDisjunctiveNormalForm());
        DBDAttributeConstraint[] constraints = filter.getConstraints();
        assertEquals(2, constraints.length);
        assertConstraint(constraints[0], targetTripId, 1, 2);
        assertConstraint(constraints[1], targetSegmentId, 10, 20);
    }

    @NotNull
    private static DBSEntityAssociation compositeAssociation(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<DBSEntityAttribute> sourceAttributes,
        @NotNull List<DBSEntityAttribute> targetAttributes
    ) throws Exception {
        DBSEntityAssociation association = mock(
            DBSEntityAssociation.class,
            Mockito.withSettings().extraInterfaces(DBSEntityReferrer.class)
        );
        DBSEntityReferrer referencedConstraint = mock(DBSEntityReferrer.class);
        DBSEntity targetEntity = mock(
            DBSEntity.class,
            Mockito.withSettings().extraInterfaces(DBSDataContainer.class)
        );

        when(association.getReferencedConstraint()).thenReturn(referencedConstraint);
        when(exposeAttrRefsList(((DBSEntityReferrer) association).getAttributeReferences(monitor)))
            .thenReturn(attributeRefs(sourceAttributes));
        when(exposeAttrRefsList(referencedConstraint.getAttributeReferences(monitor)))
            .thenReturn(attributeRefs(targetAttributes));
        when(referencedConstraint.getParentObject()).thenReturn(targetEntity);
        return association;
    }

    @NotNull
    private static List<DBSEntityAttributeRef> attributeRefs(@NotNull List<DBSEntityAttribute> attributes) {
        return attributes.stream().map(DBDReferenceUtilsTest::attributeRef).toList();
    }

    @NotNull
    private static DBSEntityAttributeRef attributeRef(@NotNull DBSEntityAttribute attribute) {
        DBSEntityAttributeRef ref = mock(DBSEntityAttributeRef.class);
        when(ref.getAttribute()).thenReturn(attribute);
        return ref;
    }

    @NotNull
    private static DBDAttributeBinding bindingFor(@NotNull DBSEntityAttribute attribute) {
        DBDAttributeBinding binding = mock(DBDAttributeBinding.class);
        when(binding.matches(attribute, true)).thenReturn(true);
        when(binding.getValueHandler()).thenReturn(mock(DBDValueHandler.class));
        return binding;
    }

    private static void assertConstraint(
        @NotNull DBDAttributeConstraint constraint,
        @NotNull DBSEntityAttribute attribute,
        @NotNull Object... values
    ) {
        assertSame(attribute, constraint.getAttribute());
        assertEquals(DBCLogicalOperator.IN, constraint.getOperator());
        assertArrayEquals(values, (Object[]) constraint.getValue());
    }

    @Nullable
    private static List<DBSEntityAttributeRef> exposeAttrRefsList(@Nullable List<? extends DBSEntityAttributeRef> list) {
        return list == null ? null : List.copyOf(list);
    }
}
