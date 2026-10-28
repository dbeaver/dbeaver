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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectSettingsProvider;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.auth.SMObjectType;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class UserDBSObjectFilterUtilsTest {
    private final Map<String, String> settings = new LinkedHashMap<>();
    private DBPDataSourceContainer dataSource;

    @BeforeEach
    void setUp() throws Exception {
        DBPObjectSettingsProvider settingsProvider = Mockito.mock(DBPObjectSettingsProvider.class);
        DBPProject project = Mockito.mock(DBPProject.class, Mockito.withSettings().extraInterfaces(IAdaptable.class));
        dataSource = Mockito.mock(DBPDataSourceContainer.class);

        Mockito.when(dataSource.getId()).thenReturn("test");
        Mockito.when(dataSource.getProject()).thenReturn(project);
        Mockito.when(((IAdaptable) project).getAdapter(DBPObjectSettingsProvider.class)).thenReturn(settingsProvider);
        Mockito.when(settingsProvider.getObjectSettings(SMObjectType.datasource, "test")).thenAnswer(call -> settings);
        Mockito.doAnswer(call -> {
            settings.putAll(call.getArgument(2));
            return null;
        }).when(settingsProvider).setObjectSettings(
            Mockito.eq(SMObjectType.datasource),
            Mockito.eq("test"),
            Mockito.anyMap()
        );
        Mockito.doAnswer(call -> {
            call.<java.util.Set<String>>getArgument(2).forEach(settings::remove);
            return null;
        }).when(settingsProvider).clearObjectSettings(
            Mockito.eq(SMObjectType.datasource),
            Mockito.eq("test"),
            Mockito.anySet()
        );
    }

    @Test
    void userFiltersAreReadAndWrittenOnlyThroughObjectSettings() throws Exception {
        DBSObjectFilter filter = new DBSObjectFilter();
        filter.setInclude(List.of("prod_%"));
        filter.setEnabled(false);

        UserDBSObjectFilterUtils.setUserObjectFilter(dataSource, CharSequence.class, null, filter);

        Assertions.assertEquals(1, settings.size());
        Assertions.assertTrue(settings.containsKey(UserDBSObjectFilterUtils.USER_FILTER_KEY));
        DBSObjectFilter restored = UserDBSObjectFilterUtils.getUserObjectFilter(dataSource, String.class, null, true);
        Assertions.assertNotNull(restored);
        Assertions.assertEquals(List.of("prod_%"), restored.getInclude());
        Assertions.assertFalse(restored.isEnabled());
        Assertions.assertTrue(restored.isUserFilter());
        Assertions.assertNull(UserDBSObjectFilterUtils.getUserObjectFilter(dataSource, String.class, null, false));
    }

    @Test
    void removingLastFilterClearsCanonicalAndLegacySettings() throws Exception {
        DBSObjectFilter filter = new DBSObjectFilter();
        filter.setInclude(List.of("prod_%"));
        UserDBSObjectFilterUtils.setUserObjectFilter(dataSource, String.class, null, filter);
        settings.put(UserDBSObjectFilterUtils.USER_FILTER_KEY + ".legacy", "[]");

        UserDBSObjectFilterUtils.setUserObjectFilter(dataSource, String.class, null, null);

        Assertions.assertTrue(settings.isEmpty());
        Assertions.assertFalse(UserDBSObjectFilterUtils.hasUserFilters(dataSource));
    }

    @Test
    void canonicalSettingOverridesLegacyFilter() {
        settings.put(
            UserDBSObjectFilterUtils.USER_FILTER_KEY + ".legacy",
            "[{\"type\":\"java.lang.String\",\"enabled\":true,\"include\":[\"legacy\"]}]"
        );
        settings.put(
            UserDBSObjectFilterUtils.USER_FILTER_KEY + ".legacy-other",
            "[{\"type\":\"java.lang.Integer\",\"enabled\":true,\"include\":[\"legacy-other\"]}]"
        );
        settings.put(
            UserDBSObjectFilterUtils.USER_FILTER_KEY,
            "[{\"type\":\"java.lang.String\",\"enabled\":true,\"include\":[\"canonical\"]}]"
        );

        DBSObjectFilter filter = UserDBSObjectFilterUtils.getUserObjectFilter(dataSource, String.class, null, true);

        Assertions.assertNotNull(filter);
        Assertions.assertEquals(List.of("canonical"), filter.getInclude());
        Assertions.assertTrue(filter.isUserFilter());
        Assertions.assertNull(UserDBSObjectFilterUtils.getUserObjectFilter(dataSource, Integer.class, null, true));
    }

    @Test
    void customFilterUpdatesInheritedMapping() throws Exception {
        DBSInstance parent = Mockito.mock(DBSInstance.class);
        DBPDataSource parentDataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(parent.getName()).thenReturn("instance");
        Mockito.when(parent.getDataSource()).thenReturn(parentDataSource);
        Mockito.when(parentDataSource.getName()).thenReturn("instance");
        Mockito.when(parentDataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);

        DBSObjectFilter globalFilter = new DBSObjectFilter();
        globalFilter.setInclude(List.of("global"));
        UserDBSObjectFilterUtils.setUserObjectFilter(dataSource, CharSequence.class, null, globalFilter);

        DBSObjectFilter customFilter = new DBSObjectFilter();
        customFilter.setInclude(List.of("custom"));
        UserDBSObjectFilterUtils.setUserObjectFilter(dataSource, String.class, parent, customFilter);

        DBSObjectFilter restored = UserDBSObjectFilterUtils.getUserObjectFilter(dataSource, String.class, parent, true);
        Assertions.assertNotNull(restored);
        Assertions.assertEquals(List.of("custom"), restored.getInclude());
    }

    @Test
    void legacyConcreteCustomFilterCanBeUpdatedAndRemoved() throws Exception {
        DBSInstance parent = Mockito.mock(DBSInstance.class);
        DBPDataSource parentDataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(parent.getName()).thenReturn("instance");
        Mockito.when(parent.getDataSource()).thenReturn(parentDataSource);
        Mockito.when(parentDataSource.getName()).thenReturn("instance");
        Mockito.when(parentDataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);

        Assertions.assertEquals("\"instance\"", FilterMapping.getFilterContainerUniqueID(parent));
        settings.put(
            UserDBSObjectFilterUtils.USER_FILTER_KEY,
            "[" +
                "{\"type\":\"java.lang.CharSequence\",\"enabled\":true,\"include\":[\"global\"]}," +
                "{\"id\":\"\\\"instance\\\"\",\"type\":\"java.lang.String\",\"enabled\":true," +
                "\"include\":[\"custom\"]}" +
                "]"
        );

        DBSObjectFilter updatedFilter = new DBSObjectFilter();
        updatedFilter.setInclude(List.of("updated"));
        UserDBSObjectFilterUtils.setUserObjectFilter(dataSource, String.class, parent, updatedFilter);

        DBSObjectFilter restored = UserDBSObjectFilterUtils.getUserObjectFilter(dataSource, String.class, parent, false);
        Assertions.assertNotNull(restored);
        Assertions.assertEquals(List.of("updated"), restored.getInclude());

        UserDBSObjectFilterUtils.setUserObjectFilter(dataSource, String.class, parent, null);

        Assertions.assertFalse(settings.get(UserDBSObjectFilterUtils.USER_FILTER_KEY).contains("custom"));
        Assertions.assertFalse(settings.get(UserDBSObjectFilterUtils.USER_FILTER_KEY).contains("updated"));
        restored = UserDBSObjectFilterUtils.getUserObjectFilter(dataSource, String.class, parent, false);
        Assertions.assertNotNull(restored);
        Assertions.assertEquals(List.of("global"), restored.getInclude());
    }
}
