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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FilterMappingTest extends DBeaverUnitTest {
    private final Map<String, FilterMapping> filters = new LinkedHashMap<>();
    private DBSInstance parent;
    private String objectId;

    @BeforeEach
    void setUp() {
        parent = Mockito.mock(DBSInstance.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(parent.getName()).thenReturn("instance");
        Mockito.when(parent.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getName()).thenReturn("instance");
        Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
        objectId = FilterMapping.getFilterContainerUniqueID(parent);
    }

    @Test
    void emptyMappingsDoNotResolve() {
        Assertions.assertNull(FilterMapping.resolve(filters, Concrete.class, null, false));
        Assertions.assertNull(FilterMapping.resolveUser(filters, Concrete.class, parent, true));
        Assertions.assertNull(FilterMapping.findMapping(filters, Concrete.class));
        Assertions.assertNull(FilterMapping.findCustomMapping(filters, Concrete.class, objectId));
    }

    @Test
    void interfacesPrecedeConcreteAndSuperclassesInDeclarationOrder() {
        addGlobal(Concrete.class);
        addGlobal(Base.class);
        addGlobal(Second.class);
        FilterMapping first = addGlobal(First.class);

        Assertions.assertSame(first, FilterMapping.resolve(filters, Concrete.class, null, false));
        Assertions.assertSame(first, FilterMapping.findMapping(filters, Concrete.class));
        filters.remove(First.class.getName());
        Assertions.assertSame(filters.get(Second.class.getName()), FilterMapping.resolve(filters, Concrete.class, null, false));
    }

    @Test
    void concretePrecedesSuperclassAndItsInterfaces() {
        FilterMapping inheritedInterface = addGlobal(BaseInterface.class);
        FilterMapping base = addGlobal(Base.class);
        FilterMapping concrete = addGlobal(Concrete.class);

        Assertions.assertSame(concrete, FilterMapping.resolve(filters, Concrete.class, null, false));
        filters.remove(Concrete.class.getName());
        Assertions.assertSame(base, FilterMapping.resolve(filters, Concrete.class, null, false));
        filters.remove(Base.class.getName());
        Assertions.assertSame(inheritedInterface, FilterMapping.resolve(filters, Concrete.class, null, false));
        Assertions.assertSame(inheritedInterface, FilterMapping.findMapping(filters, Concrete.class));
    }

    @Test
    void datasourceInterfaceSearchIsShallowButUserSelectionIsRecursive() {
        FilterMapping root = addGlobal(Root.class);

        Assertions.assertSame(root, FilterMapping.resolve(filters, Concrete.class, null, false));
        Assertions.assertNull(FilterMapping.resolve(filters, DeepConcrete.class, null, false));
        Assertions.assertSame(root, FilterMapping.findMapping(filters, DeepConcrete.class));
        root.customFilters.put(objectId, filter());
        Assertions.assertSame(root, FilterMapping.findCustomMapping(filters, DeepConcrete.class, objectId));
        Assertions.assertSame(root, FilterMapping.resolveUser(filters, DeepConcrete.class, parent, false));
        Assertions.assertNull(FilterMapping.resolve(filters, DeepConcrete.class, parent, false));
    }

    @Test
    void existingTypeMappingBlocksItsImmediateInterfaceFallback() {
        addGlobal(Root.class);
        FilterMapping first = addGlobal(First.class);
        first.globalFilter.setEnabled(false);
        FilterMapping concrete = addGlobal(Concrete.class);

        Assertions.assertSame(concrete, FilterMapping.resolve(filters, Concrete.class, null, false));
        Assertions.assertSame(first, FilterMapping.resolve(filters, Concrete.class, null, true));
        filters.remove(First.class.getName());
        Assertions.assertSame(filters.get(Root.class.getName()), FilterMapping.resolve(filters, Concrete.class, null, false));
    }

    @Test
    void disabledInterfaceDoesNotBlockLaterInterface() {
        FilterMapping first = addGlobal(First.class);
        first.globalFilter.setEnabled(false);
        FilterMapping second = addGlobal(Second.class);

        Assertions.assertSame(second, FilterMapping.resolve(filters, Concrete.class, null, false));
        Assertions.assertSame(first, FilterMapping.resolve(filters, Concrete.class, null, true));
    }

    @Test
    void firstMatchWithParentRequiresLocalFilter() {
        FilterMapping mapping = addGlobal(First.class);
        mapping.customFilters.put("other-parent", filter());

        Assertions.assertNull(FilterMapping.resolve(filters, Concrete.class, parent, true));
        Assertions.assertSame(mapping, FilterMapping.resolve(filters, Concrete.class, parent, false));
        Assertions.assertSame(mapping.globalFilter, mapping.getFilter(parent, false));
        Assertions.assertSame(mapping, FilterMapping.resolve(filters, Concrete.class, null, true));
    }

    @Test
    void applicableCustomFilterPrecedesGlobalWithinMapping() {
        FilterMapping mapping = addGlobal(First.class);
        DBSObjectFilter custom = filter();
        mapping.customFilters.put(objectId, custom);

        Assertions.assertSame(mapping, FilterMapping.resolve(filters, Concrete.class, parent, false));
        Assertions.assertSame(custom, mapping.getFilter(parent, false));
        Assertions.assertSame(custom, mapping.getFilter(parent, true));
        Assertions.assertSame(mapping.globalFilter, mapping.getFilter(null, true));
    }

    @Test
    void disabledCustomIsReturnedForFirstMatchAndOtherwiseFallsBackToGlobal() {
        FilterMapping mapping = addGlobal(First.class);
        DBSObjectFilter custom = filter();
        custom.setEnabled(false);
        mapping.customFilters.put(objectId, custom);

        Assertions.assertSame(mapping, FilterMapping.resolve(filters, Concrete.class, parent, true));
        Assertions.assertSame(custom, mapping.getFilter(parent, true));
        Assertions.assertSame(mapping, FilterMapping.resolve(filters, Concrete.class, parent, false));
        Assertions.assertSame(mapping.globalFilter, mapping.getFilter(parent, false));
        mapping.globalFilter = null;
        Assertions.assertNull(FilterMapping.resolve(filters, Concrete.class, parent, false));
    }

    @Test
    void emptyCustomFallsBackButEnabledEmptyGlobalStillResolves() {
        FilterMapping mapping = addGlobal(First.class);
        DBSObjectFilter empty = new DBSObjectFilter();
        mapping.customFilters.put(objectId, empty);

        Assertions.assertSame(empty, mapping.getFilter(parent, true));
        Assertions.assertSame(mapping.globalFilter, mapping.getFilter(parent, false));
        mapping.globalFilter = new DBSObjectFilter();
        Assertions.assertSame(mapping, FilterMapping.resolve(filters, Concrete.class, parent, false));
        Assertions.assertSame(mapping, FilterMapping.resolve(filters, Concrete.class, null, false));
    }

    @Test
    void userCustomPriorityDoesNotChangeDatasourceInterfacePriority() {
        FilterMapping interfaceMapping = addGlobal(First.class);
        FilterMapping concrete = addGlobal(Concrete.class);
        concrete.customFilters.put(objectId, filter());

        Assertions.assertSame(interfaceMapping, FilterMapping.resolve(filters, Concrete.class, parent, false));
        Assertions.assertSame(concrete, FilterMapping.resolveUser(filters, Concrete.class, parent, false));
        Assertions.assertSame(concrete, FilterMapping.resolveUser(filters, Concrete.class, parent, true));
        Assertions.assertSame(interfaceMapping, FilterMapping.resolveUser(filters, Concrete.class, null, false));
    }

    @Test
    void userCustomPriorityUsesExistingGlobalFallbackForEmptyOrDisabledCustom() {
        FilterMapping interfaceMapping = addGlobal(First.class);
        FilterMapping concrete = addGlobal(Concrete.class);
        DBSObjectFilter custom = new DBSObjectFilter();
        concrete.customFilters.put(objectId, custom);

        Assertions.assertSame(concrete, FilterMapping.resolveUser(filters, Concrete.class, parent, false));
        custom.setEnabled(false);
        Assertions.assertSame(concrete, FilterMapping.resolveUser(filters, Concrete.class, parent, true));
        Assertions.assertSame(concrete, FilterMapping.resolveUser(filters, Concrete.class, parent, false));
        concrete.globalFilter = null;
        Assertions.assertSame(interfaceMapping, FilterMapping.resolveUser(filters, Concrete.class, parent, false));
    }

    @Test
    void userLookupFallsBackAfterFirstCustomCandidateWithoutSearchingForAnotherDeepCustom() {
        FilterMapping first = addGlobal(First.class);
        first.globalFilter = null;
        DBSObjectFilter disabled = filter();
        disabled.setEnabled(false);
        first.customFilters.put(objectId, disabled);
        FilterMapping root = addGlobal(Root.class);
        root.customFilters.put(objectId, filter());

        Assertions.assertSame(first, FilterMapping.findCustomMapping(filters, Concrete.class, objectId));
        Assertions.assertNull(FilterMapping.resolveUser(filters, Concrete.class, parent, false));
        Assertions.assertSame(first, FilterMapping.resolveUser(filters, Concrete.class, parent, true));
    }

    @Test
    void userWriteSelectionIgnoresApplicabilityAndOnlyMatchesRequestedParent() {
        FilterMapping first = addGlobal(First.class);
        first.globalFilter = null;
        first.customFilters.put("other-parent", filter());
        FilterMapping concrete = addGlobal(Concrete.class);
        DBSObjectFilter disabled = filter();
        disabled.setEnabled(false);
        concrete.customFilters.put(objectId, disabled);

        Assertions.assertSame(first, FilterMapping.findMapping(filters, Concrete.class));
        Assertions.assertSame(concrete, FilterMapping.findCustomMapping(filters, Concrete.class, objectId));
        concrete.customFilters.put(objectId, new DBSObjectFilter());
        Assertions.assertSame(concrete, FilterMapping.findCustomMapping(filters, Concrete.class, objectId));
        concrete.customFilters.put(objectId, null);
        Assertions.assertSame(concrete, FilterMapping.findCustomMapping(filters, Concrete.class, objectId));
        concrete.customFilters.remove(objectId);
        Assertions.assertNull(FilterMapping.findCustomMapping(filters, Concrete.class, objectId));
    }

    @NotNull
    private FilterMapping addGlobal(@NotNull Class<?> type) {
        FilterMapping mapping = new FilterMapping(type.getName());
        mapping.globalFilter = filter();
        filters.put(type.getName(), mapping);
        return mapping;
    }

    @NotNull
    private static DBSObjectFilter filter() {
        DBSObjectFilter filter = new DBSObjectFilter();
        filter.setInclude(List.of("included"));
        return filter;
    }

    private interface Root {
    }

    private interface First extends Root {
    }

    private interface Deep extends First {
    }

    private interface Second {
    }

    private interface BaseInterface {
    }

    private static class Base implements BaseInterface {
    }

    private static class Concrete extends Base implements First, Second {
    }

    private static class DeepConcrete implements Deep {
    }
}
