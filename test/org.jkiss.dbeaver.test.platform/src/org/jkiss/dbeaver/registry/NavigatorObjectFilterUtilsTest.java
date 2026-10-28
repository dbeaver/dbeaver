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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectSettingsProvider;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.auth.SMObjectType;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.registry.project.BaseProjectSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Isolated
class NavigatorObjectFilterUtilsTest {
    private static final String OBJECT_ID = "test-datasource";
    private static final String FILTER_KEY = UserDBSObjectFilterUtils.USER_FILTER_KEY;
    private static final String LEGACY_FILTER_KEY = FILTER_KEY + ".legacy";

    private DBPProject project;
    private DBPDataSourceRegistry registry;
    private DBPDataSourceContainer container;
    private DBNModel model;
    private DBNDatabaseNode node;
    private DBXTreeItem meta;
    private BaseProjectSettings settings;
    private ProgressProvider previousProgressProvider;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        project = Mockito.mock(DBPProject.class, Mockito.withSettings().extraInterfaces(IAdaptable.class));
        registry = Mockito.mock(DBPDataSourceRegistry.class);
        container = Mockito.mock(DBPDataSourceContainer.class);
        model = Mockito.mock(DBNModel.class);
        node = Mockito.mock(DBNDatabaseNode.class);
        meta = Mockito.mock(DBXTreeItem.class);
        settings = new TestProjectSettings(project);
        Mockito.when(project.getNavigatorModel()).thenReturn(model);
        Mockito.when(project.getDataSourceRegistry()).thenReturn(registry);
        Mockito.when(registry.getDataSource(OBJECT_ID)).thenReturn(container);
        Mockito.when(model.getNodeByObject(container)).thenReturn(node);
        Mockito.when(container.getId()).thenReturn(OBJECT_ID);
        Mockito.when(container.getProject()).thenReturn(project);
        Mockito.when(container.getRegistry()).thenReturn(registry);
        Mockito.when(((IAdaptable) project).getAdapter(DBPObjectSettingsProvider.class)).thenReturn(settings);
        Mockito.when(node.getDataSourceContainer()).thenReturn(container);
        // IJobManager has a setter but no getter; preserve the provider installed by the surrounding test runtime.
        Field progressProvider = Job.getJobManager().getClass().getDeclaredField("progressProvider");
        progressProvider.setAccessible(true);
        previousProgressProvider = (ProgressProvider) progressProvider.get(Job.getJobManager());
        Job.getJobManager().setProgressProvider(new ProgressProvider() {
            @NotNull
            @Override
            public IProgressMonitor createMonitor(@NotNull Job job) {
                if (job.belongsTo(NavigatorObjectFilterUtils.class)) {
                    // RuntimeUtils reuses DBRProgressMonitor directly, avoiding a platform-dependent shutdown check.
                    return Mockito.mock(IProgressMonitor.class, Mockito.withSettings().extraInterfaces(DBRProgressMonitor.class));
                }
                return previousProgressProvider == null
                    ? new NullProgressMonitor() : previousProgressProvider.createMonitor(job);
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            joinFilterJobs();
            Mockito.verify(registry, Mockito.never()).refreshConfig();
            Mockito.verify(registry, Mockito.never()).refreshConfig(Mockito.any());
        } finally {
            Job.getJobManager().setProgressProvider(previousProgressProvider);
        }
    }

    @Test
    void canonicalAndLegacyCacheUpdatesRefilterOnBackgroundThread() throws Exception {
        settings.getObjectSettings(SMObjectType.datasource, OBJECT_ID);
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> updateThread = new AtomicReference<>();
        AtomicReference<Job> updateJob = new AtomicReference<>();
        Mockito.doAnswer(call -> {
            updateThread.set(Thread.currentThread());
            updateJob.set(Job.getJobManager().currentJob());
            return null;
        }).when(node).updateChildrenOrder(true);

        int updates = 0;
        for (String key : List.of(FILTER_KEY, LEGACY_FILTER_KEY)) {
            settings.updateObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Map.of(key, "[]"));
            joinFilterJobs();

            Assertions.assertEquals("[]", settings.getObjectSettings(SMObjectType.datasource, OBJECT_ID).get(key));
            Mockito.verify(node, Mockito.times(++updates)).updateChildrenOrder(true);
            Assertions.assertNotNull(updateThread.get());
            Assertions.assertNotSame(caller, updateThread.get());
            Assertions.assertInstanceOf(AbstractJob.class, updateJob.get());
            Assertions.assertTrue(updateJob.get().belongsTo(NavigatorObjectFilterUtils.class));
            Assertions.assertTrue(updateJob.get().isSystem());
        }
    }

    @Test
    void canonicalAndLegacyCacheDeletesRefilterLoadedNode() throws Exception {
        settings.getObjectSettings(SMObjectType.datasource, OBJECT_ID);
        int updates = 0;
        for (String key : List.of(FILTER_KEY, LEGACY_FILTER_KEY)) {
            settings.updateObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Map.of(key, "[]"));
            joinFilterJobs();
            Mockito.verify(node, Mockito.times(++updates)).updateChildrenOrder(true);

            settings.deleteObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Set.of(key));
            joinFilterJobs();

            Assertions.assertFalse(settings.getObjectSettings(SMObjectType.datasource, OBJECT_ID).containsKey(key));
            Mockito.verify(node, Mockito.times(++updates)).updateChildrenOrder(true);
        }
    }

    @Test
    void deleteAllUsesFilterKeysFromLoadedSettingsCache() throws Exception {
        settings.getObjectSettings(SMObjectType.datasource, OBJECT_ID);
        settings.updateObjectSettingsCache(
            SMObjectType.datasource, OBJECT_ID, Map.of(FILTER_KEY, "[]", LEGACY_FILTER_KEY, "[]", "unrelated", "value"));
        joinFilterJobs();

        settings.deleteObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, null);
        joinFilterJobs();

        Assertions.assertNull(settings.getObjectSettings(SMObjectType.datasource, OBJECT_ID));
        Mockito.verify(node, Mockito.times(2)).updateChildrenOrder(true);
    }

    @Test
    void deleteAllWithUninitializedSettingsCacheStillRefiltersLoadedNode() throws Exception {
        settings.deleteObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, null);
        joinFilterJobs();

        Mockito.verify(node).updateChildrenOrder(true);
    }

    @Test
    void unrelatedAndEmptyChangesDoNotRefilter() throws Exception {
        settings.getObjectSettings(SMObjectType.datasource, OBJECT_ID);
        settings.updateObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Map.of("unrelated", "value"));
        joinFilterJobs();
        settings.deleteObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Set.of("unrelated"));
        joinFilterJobs();
        settings.updateObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Map.of());
        joinFilterJobs();
        settings.deleteObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Set.of());
        joinFilterJobs();
        settings.deleteObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, null);
        joinFilterJobs();

        Mockito.verifyNoInteractions(model, node);
    }

    @Test
    void deleteAllOfUnrelatedCachedSettingsDoesNotRefilter() throws Exception {
        settings.getObjectSettings(SMObjectType.datasource, OBJECT_ID);
        settings.updateObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Map.of("unrelated", "value"));
        joinFilterJobs();

        settings.deleteObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, null);
        joinFilterJobs();

        Assertions.assertNull(settings.getObjectSettings(SMObjectType.datasource, OBJECT_ID));
        Mockito.verifyNoInteractions(model, node);
    }

    @Test
    void projectSettingsDoNotRefilterDatasourceWithSameId() throws Exception {
        settings.updateObjectSettingsCache(SMObjectType.project, OBJECT_ID, Map.of(FILTER_KEY, "[]"));
        joinFilterJobs();
        settings.deleteObjectSettingsCache(SMObjectType.project, OBJECT_ID, Set.of(FILTER_KEY));
        joinFilterJobs();
        settings.deleteObjectSettingsCache(SMObjectType.project, OBJECT_ID, null);
        joinFilterJobs();

        Mockito.verifyNoInteractions(model, node);
    }

    @Test
    void absentProjectModelIsSafe() throws Exception {
        Mockito.when(project.getNavigatorModel()).thenReturn(null);

        mutateFilterCache();

        Mockito.verifyNoInteractions(model, node);
    }

    @Test
    void missingDatasourceIsSafe() throws Exception {
        Mockito.when(registry.getDataSource(OBJECT_ID)).thenReturn(null);

        mutateFilterCache();

        Mockito.verifyNoInteractions(model, node);
    }

    @Test
    void absentNavigatorNodeIsNotLoaded() throws Exception {
        Mockito.when(model.getNodeByObject(container)).thenReturn(null);

        mutateFilterCache();

        Mockito.verify(model, Mockito.times(3)).getNodeByObject(container);
        Mockito.verifyNoMoreInteractions(model);
        Mockito.verifyNoInteractions(node);
    }

    @Test
    void uninitializedNavigatorNodeIsNotRefiltered() throws Exception {
        Mockito.when(node.needsInitialization()).thenReturn(true);

        mutateFilterCache();

        Mockito.verify(node, Mockito.never()).updateChildrenOrder(Mockito.anyBoolean());
    }

    @Test
    void disposedNavigatorNodeIsNotRefiltered() throws Exception {
        Mockito.when(node.isDisposed()).thenReturn(true);

        mutateFilterCache();

        Mockito.verify(node, Mockito.never()).updateChildrenOrder(Mockito.anyBoolean());
    }

    @Test
    void backgroundJobResolvesCurrentNodeAgain() throws Exception {
        DBNDatabaseNode replacement = Mockito.mock(DBNDatabaseNode.class);
        // The first lookup checks eligibility; the job must use the second lookup's node.
        Mockito.when(model.getNodeByObject(container)).thenReturn(node, replacement);

        settings.updateObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Map.of(FILTER_KEY, "[]"));
        joinFilterJobs();

        Mockito.verify(node, Mockito.never()).updateChildrenOrder(Mockito.anyBoolean());
        Mockito.verify(replacement).updateChildrenOrder(true);
    }

    @Test
    void nodeRemovedBeforeJobRunsIsSafe() throws Exception {
        Mockito.when(model.getNodeByObject(container)).thenReturn(node, null);

        settings.updateObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Map.of(FILTER_KEY, "[]"));
        joinFilterJobs();

        Mockito.verify(model, Mockito.times(2)).getNodeByObject(container);
        Mockito.verify(node, Mockito.never()).updateChildrenOrder(Mockito.anyBoolean());
    }

    @Test
    void nodeDisposedBeforeJobRunsIsSafe() throws Exception {
        Mockito.when(node.isDisposed()).thenReturn(false, true);

        settings.updateObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Map.of(FILTER_KEY, "[]"));
        joinFilterJobs();

        Mockito.verify(node, Mockito.times(2)).isDisposed();
        Mockito.verify(node, Mockito.never()).updateChildrenOrder(Mockito.anyBoolean());
    }

    @Test
    void editingRetainsDisabledPersistedLocalFilterWhileEffectiveFilterUsesGlobal() throws Exception {
        DBSObjectFilter local = filter("local_%");
        local.setEnabled(false);

        assertDefinedLocalAndEffectiveGlobal(local);
    }

    @Test
    void editingRetainsEmptyPersistedLocalFilterWhileEffectiveFilterUsesGlobal() throws Exception {
        assertDefinedLocalAndEffectiveGlobal(new DBSObjectFilter());
    }

    @Test
    void effectiveFilterUsesDatasourceCaseSensitivity() {
        Mockito.doReturn(String.class).when(node).getChildrenOrFolderClass(meta);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(node.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        try (MockedStatic<UserDBSObjectFilterUtils> filters = mockUserFilters()) {
            for (boolean caseSensitive : List.of(true, false)) {
                DBSObjectFilter filter = filter("Prod_%");
                filter.setCaseSensitive(!caseSensitive);
                filters.when(() -> UserDBSObjectFilterUtils.getUserObjectFilter(container, String.class, null, false))
                    .thenReturn(filter);
                Mockito.when(dialect.hasCaseSensitiveFiltration()).thenReturn(caseSensitive);

                Assertions.assertSame(filter, NavigatorObjectFilterUtils.getEffectiveUserFilter(node, meta));
                Assertions.assertEquals(caseSensitive, filter.isCaseSensitive());
                Assertions.assertEquals(!caseSensitive, filter.matches("prod_table"));
            }
        }
    }

    @Test
    void effectiveFilterWithoutDatasourceKeepsMatchingRules() {
        Mockito.doReturn(String.class).when(node).getChildrenOrFolderClass(null);
        DBSObjectFilter filter = filter("Prod_%");
        filter.setCaseSensitive(true);
        try (MockedStatic<UserDBSObjectFilterUtils> filters = mockUserFilters()) {
            filters.when(() -> UserDBSObjectFilterUtils.getUserObjectFilter(container, String.class, null, false))
                .thenReturn(filter);

            Assertions.assertSame(filter, NavigatorObjectFilterUtils.getEffectiveUserFilter(node, null));
            Assertions.assertTrue(filter.isCaseSensitive());
        }
    }

    @Test
    void filterProviderIntersectsConnectionAndEffectivePersonalFilters() throws Exception {
        DBSObjectFilter local = filter("local_%");
        local.setEnabled(false);
        assertDefinedLocalAndEffectiveGlobal(local);
        DBSObjectFilter connection = filter("%");
        connection.setExclude(List.of("global_hidden"));

        Predicate<DBSObject> matcher = NavigatorObjectFilterUtils.FILTER_PROVIDER.createFilter(node, meta, connection);

        Assertions.assertNotNull(matcher);
        Assertions.assertTrue(matcher.test(object("global_visible")));
        Assertions.assertFalse(matcher.test(object("global_hidden")));
        Assertions.assertFalse(matcher.test(object("other")));
        Assertions.assertFalse(matcher.test(object("local_visible")));
        matcher = NavigatorObjectFilterUtils.FILTER_PROVIDER.createFilter(node, meta, null);
        Assertions.assertNotNull(matcher);
        Assertions.assertTrue(matcher.test(object("global_hidden")));
        Assertions.assertFalse(matcher.test(object("other")));
    }

    @Test
    void filterProviderWithoutPersonalFilterStillAppliesConnectionFilter() {
        Mockito.doReturn(String.class).when(node).getChildrenOrFolderClass(meta);

        Predicate<DBSObject> matcher = NavigatorObjectFilterUtils.FILTER_PROVIDER.createFilter(node, meta, filter("shared_%"));

        Assertions.assertNotNull(matcher);
        Assertions.assertTrue(matcher.test(object("shared_visible")));
        Assertions.assertFalse(matcher.test(object("other")));
        Assertions.assertNull(NavigatorObjectFilterUtils.getEffectiveUserFilter(node, meta));
        Assertions.assertNull(NavigatorObjectFilterUtils.FILTER_PROVIDER.createFilter(node, meta, null));
    }

    @Test
    void setUserFilterDelegatesValueAndRemovalWithNodeContext() throws Exception {
        DBSObject parent = Mockito.mock(DBSObject.class);
        DBSObjectFilter filter = filter("local_%");
        Mockito.doReturn(String.class).when(node).getChildrenOrFolderClass(null);
        Mockito.when(node.getFilterObject()).thenReturn(parent);
        try (MockedStatic<UserDBSObjectFilterUtils> filters = mockUserFilters()) {
            Assertions.assertTrue(NavigatorObjectFilterUtils.setUserFilter(node, null, filter));
            filters.verify(() -> UserDBSObjectFilterUtils.setUserObjectFilter(container, String.class, parent, filter));

            Assertions.assertTrue(NavigatorObjectFilterUtils.setUserFilter(node, null, null));
            filters.verify(() -> UserDBSObjectFilterUtils.setUserObjectFilter(container, String.class, parent, null));
            filters.verifyNoMoreInteractions();
        }
    }

    @Test
    void unsupportedNodeDoesNotReadOrWriteUserFilters() throws Exception {
        try (MockedStatic<UserDBSObjectFilterUtils> filters = mockUserFilters()) {
            Assertions.assertNull(NavigatorObjectFilterUtils.getUserFilter(node, meta));
            Assertions.assertNull(NavigatorObjectFilterUtils.getEffectiveUserFilter(node, meta));
            Assertions.assertFalse(NavigatorObjectFilterUtils.setUserFilter(node, meta, filter("unused")));
            Assertions.assertFalse(NavigatorObjectFilterUtils.setUserFilter(node, meta, null));

            filters.verifyNoInteractions();
        }
    }

    @Test
    void setUserFilterPropagatesSettingsFailure() {
        Mockito.doReturn(String.class).when(node).getChildrenOrFolderClass(meta);
        DBSObjectFilter filter = filter("local_%");
        DBException failure = new DBException("Cannot save filter");
        try (MockedStatic<UserDBSObjectFilterUtils> filters = mockUserFilters()) {
            filters.when(() -> UserDBSObjectFilterUtils.setUserObjectFilter(container, String.class, null, filter))
                .thenThrow(failure);

            Assertions.assertSame(failure, Assertions.assertThrows(
                DBException.class, () -> NavigatorObjectFilterUtils.setUserFilter(node, meta, filter)));
        }
    }

    private void assertDefinedLocalAndEffectiveGlobal(@NotNull DBSObjectFilter local) throws Exception {
        Mockito.doReturn(String.class).when(node).getChildrenOrFolderClass(meta);
        Assertions.assertTrue(NavigatorObjectFilterUtils.setUserFilter(node, meta, filter("global_%")));
        joinFilterJobs();
        DBSInstance parent = Mockito.mock(DBSInstance.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(parent.getName()).thenReturn("instance");
        Mockito.when(parent.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getName()).thenReturn("instance");
        Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
        Mockito.when(node.getFilterObject()).thenReturn(parent);
        Assertions.assertNull(NavigatorObjectFilterUtils.getUserFilter(node, meta));

        Assertions.assertTrue(NavigatorObjectFilterUtils.setUserFilter(node, meta, local));
        joinFilterJobs();

        DBSObjectFilter defined = NavigatorObjectFilterUtils.getUserFilter(node, meta);
        Assertions.assertNotNull(defined);
        Assertions.assertNotSame(local, defined);
        // JSON persistence normalizes an absent pattern list to an empty list.
        Assertions.assertEquals(local.getInclude() == null ? List.of() : local.getInclude(), defined.getInclude());
        Assertions.assertEquals(local.isEnabled(), defined.isEnabled());
        Assertions.assertTrue(defined.isUserFilter());
        DBSObjectFilter effective = NavigatorObjectFilterUtils.getEffectiveUserFilter(node, meta);
        Assertions.assertNotNull(effective);
        Assertions.assertEquals(List.of("global_%"), effective.getInclude());
        Assertions.assertTrue(effective.isEnabled());
        Assertions.assertTrue(effective.isUserFilter());
    }

    private void mutateFilterCache() throws Exception {
        settings.updateObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Map.of(FILTER_KEY, "[]"));
        joinFilterJobs();
        settings.deleteObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, Set.of(FILTER_KEY));
        joinFilterJobs();
        settings.deleteObjectSettingsCache(SMObjectType.datasource, OBJECT_ID, null);
        joinFilterJobs();
    }

    private static void joinFilterJobs() throws InterruptedException {
        Job.getJobManager().join(NavigatorObjectFilterUtils.class, null);
    }

    @NotNull
    private static MockedStatic<UserDBSObjectFilterUtils> mockUserFilters() {
        return Mockito.mockStatic(UserDBSObjectFilterUtils.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
    }

    @NotNull
    private static DBSObjectFilter filter(@NotNull String include) {
        DBSObjectFilter filter = new DBSObjectFilter();
        filter.setInclude(List.of(include));
        return filter;
    }

    @NotNull
    private static DBSObject object(@NotNull String name) {
        DBSObject object = Mockito.mock(DBSObject.class);
        Mockito.when(object.getName()).thenReturn(name);
        return object;
    }

    private static class TestProjectSettings extends BaseProjectSettings {
        private TestProjectSettings(@NotNull DBPProject project) {
            super(project);
        }

        @NotNull
        @Override
        protected Map<SMObjectType, Map<String, Map<String, String>>> loadAllProjectSettings() {
            return new LinkedHashMap<>();
        }

        @Override
        protected void saveProjectSettings(
            @NotNull SMObjectType objectType,
            @NotNull String objectId,
            @NotNull Map<String, String> settings
        ) {
            // Persistence is outside this test; BaseProjectSettings owns the real cache and notifications.
        }
    }
}
