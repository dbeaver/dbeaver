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
package org.jkiss.dbeaver.model.access;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBInfoUtils;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceOriginLocal;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.StringReader;
import java.util.Map;

public class DBAuthUtilsTest {
    private final DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
    private final DBPProject project = Mockito.mock(DBPProject.class);
    private final DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
    private final DBPPlatform platform = Mockito.mock(DBPPlatform.class);
    private final DBPApplication application = Mockito.mock(DBPApplication.class);
    private final DBPPlatformUI platformUI = Mockito.mock(DBPPlatformUI.class);
    private final DBSSecretController secretController = Mockito.mock(DBSSecretController.class);
    private final DBAUserPasswordManager manager = Mockito.mock(DBAUserPasswordManager.class);
    private final DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
    private final DBPConnectionConfiguration configured = new DBPConnectionConfiguration();
    private final DBPConnectionConfiguration actual = new DBPConnectionConfiguration();
    private MockedStatic<DBWorkbench> workbench;
    private MockedStatic<DBSSecretController> secretControllers;

    @BeforeEach
    public void setUp() throws DBException {
        workbench = Mockito.mockStatic(DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        secretControllers = Mockito.mockStatic(DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
        workbench.when(DBWorkbench::getPlatformUI).thenReturn(platformUI);
        workbench.when(DBWorkbench::isDistributed).thenReturn(true);
        secretControllers.when(() -> DBSSecretController.getProjectSecretController(project)).thenReturn(secretController);
        Mockito.when(platform.getApplication()).thenReturn(application);
        Mockito.when(container.getProject()).thenReturn(project);
        Mockito.when(project.getWorkspace()).thenReturn(workspace);
        Mockito.when(container.getConnectionConfiguration()).thenReturn(configured);
        Mockito.when(container.getActualConnectionConfiguration()).thenReturn(actual);
        Mockito.when(project.isUseSecretStorage()).thenReturn(true);
        Mockito.when(secretController.getSupportedFeatures()).thenReturn(DBSSecretController.FEATURE_PRIVATE_SECRETS_EDIT);
        configured.setUserName("configured-user");
        configured.setUserPassword("configured-password");
        actual.setUserName("connected-user");
        actual.setUserPassword("connected-password");
        DBAPasswordChangeInfo passwordInfo = new DBAPasswordChangeInfo("connected-user", "entered-old-password");
        passwordInfo.setNewPassword("new-password");
        Mockito.when(platformUI.promptUserPasswordChange(
            Mockito.any(), Mockito.eq("connected-user"), Mockito.eq("connected-password"), Mockito.eq(false), Mockito.eq(true)
        )).thenReturn(passwordInfo);
        Mockito.when(platformUI.confirmAction(Mockito.any(), Mockito.any())).thenReturn(true);
    }

    @AfterEach
    public void tearDown() {
        secretControllers.close();
        workbench.close();
    }

    @Test
    public void managerCanChangePersonalPasswordInSharedProject() {
        Mockito.when(project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_VIEW)).thenReturn(true);
        Mockito.when(workspace.hasRealmPermission(RMConstants.PERMISSION_SQL_EXECUTE_QUERY)).thenReturn(true);
        Assertions.assertTrue(DBAuthUtils.canChangeCurrentUserPassword(container));
    }

    @Test
    public void managerCannotChangeSharedPassword() {
        Mockito.when(project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_VIEW)).thenReturn(true);
        Mockito.when(workspace.hasRealmPermission(RMConstants.PERMISSION_SQL_EXECUTE_QUERY)).thenReturn(true);
        Mockito.when(container.isSharedCredentials()).thenReturn(true);
        Assertions.assertFalse(DBAuthUtils.canChangeCurrentUserPassword(container));
    }

    @Test
    public void viewingConnectionDoesNotGrantPasswordChange() {
        Mockito.when(project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_VIEW)).thenReturn(true);
        Assertions.assertFalse(DBAuthUtils.canChangeCurrentUserPassword(container));
    }

    @Test
    public void sqlPermissionDoesNotGrantAccessToAnotherProject() {
        Mockito.when(workspace.hasRealmPermission(RMConstants.PERMISSION_SQL_EXECUTE_QUERY)).thenReturn(true);
        Assertions.assertFalse(DBAuthUtils.canChangeCurrentUserPassword(container));
    }

    @Test
    public void existingConnectionEditorsRetainAccess() {
        Mockito.when(project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT)).thenReturn(true);
        Mockito.when(container.isSharedCredentials()).thenReturn(true);
        Assertions.assertTrue(DBAuthUtils.canChangeCurrentUserPassword(container));
    }

    @Test
    public void managerCannotChangeCachedSharedPasswordAfterRevocation() {
        Mockito.when(project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_VIEW)).thenReturn(true);
        Mockito.when(workspace.hasRealmPermission(RMConstants.PERMISSION_SQL_EXECUTE_QUERY)).thenReturn(true);
        DataSourceDescriptor descriptor = createDescriptor();
        descriptor.forceSetSharedCredentials(true);
        descriptor.setSavePassword(true);
        Mockito.doReturn(true).when(descriptor).isConnected();

        descriptor.setSharedCredentials(false);
        descriptor.resetAllSecrets();

        Assertions.assertFalse(descriptor.isSharedCredentials());
        Assertions.assertEquals("connected-user", DBAuthUtils.getCurrentUserName(descriptor));
        Assertions.assertNull(descriptor.saveToSecret());
        Assertions.assertFalse(DBAuthUtils.canChangeCurrentUserPassword(descriptor));
    }

    @Test
    public void connectionEditorsCannotChangeCachedSharedPasswordAfterRevocation() {
        Mockito.when(project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT)).thenReturn(true);
        DataSourceDescriptor descriptor = createDescriptor();
        descriptor.forceSetSharedCredentials(true);
        Mockito.doReturn(true).when(descriptor).isConnected();

        descriptor.setSharedCredentials(false);
        descriptor.resetAllSecrets();

        Assertions.assertFalse(DBAuthUtils.canChangeCurrentUserPassword(descriptor));
    }

    @Test
    public void managerCanChangePersonalPasswordAfterRevokedCredentialsAreCleared() {
        Mockito.when(project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_VIEW)).thenReturn(true);
        Mockito.when(workspace.hasRealmPermission(RMConstants.PERMISSION_SQL_EXECUTE_QUERY)).thenReturn(true);
        DataSourceDescriptor descriptor = createDescriptor();
        descriptor.forceSetSharedCredentials(true);
        descriptor.setSavePassword(true);
        Mockito.doReturn(true).when(descriptor).isConnected();
        descriptor.setSharedCredentials(false);

        Mockito.doReturn(false).when(descriptor).isConnected();
        descriptor.resetAllSecrets();

        Assertions.assertNull(configured.getUserName());
        Assertions.assertNull(configured.getUserPassword());
        Assertions.assertFalse(descriptor.isSavePassword());
        configured.setUserName("personal-user");
        configured.setUserPassword("personal-password");
        Mockito.doReturn(configured).when(descriptor).getActualConnectionConfiguration();
        Assertions.assertEquals("personal-user", DBAuthUtils.getCurrentUserName(descriptor));
        Assertions.assertTrue(DBAuthUtils.canChangeCurrentUserPassword(descriptor));
    }

    @Test
    public void connectionEditorsCanPersistSelectedSharedCredentials() throws Exception {
        Mockito.when(project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT)).thenReturn(true);
        Mockito.when(secretController.getSupportedFeatures()).thenReturn(DBSSecretController.FEATURE_SHARED_SECRETS_EDIT);
        DataSourceDescriptor descriptor = createDescriptor();
        descriptor.forceSetSharedCredentials(true);
        descriptor.setSavePassword(true);
        descriptor.setSelectedSharedCredentials(new DBSSecretValue(
            "all_users", "test-project/datasources/test-connection", "All users",
            "{\"user\":\"connected-user\",\"password\":\"connected-password\"}"));
        Assertions.assertTrue(DBAuthUtils.canChangeCurrentUserPassword(descriptor));

        Assertions.assertTrue(DBAuthUtils.promptAndChangePasswordForCurrentUser(monitor, descriptor, manager));

        ArgumentCaptor<DBSSecretValue> secret = ArgumentCaptor.forClass(DBSSecretValue.class);
        Mockito.verify(secretController).setSubjectSecretValue(Mockito.eq("all_users"), Mockito.eq(descriptor), secret.capture());
        Map<String, Object> saved = JSONUtils.parseMap(DBInfoUtils.SECRET_GSON, new StringReader(secret.getValue().getValue()));
        Assertions.assertEquals("connected-user", saved.get("user"));
        Assertions.assertEquals("new-password", saved.get("password"));
        Mockito.verify(secretController, Mockito.never()).setPrivateSecretValue(Mockito.eq(descriptor), Mockito.any());
    }

    @Test
    public void savesPersonalSecretWithoutUpdatingSharedConnectionConfiguration() throws DBException {
        Assertions.assertTrue(changePassword());
        Mockito.verify(manager).changeUserPassword(monitor, "connected-user", "new-password", "entered-old-password");
        Mockito.verify(container).setSavePassword(true);
        Mockito.verify(container).persistSecrets(secretController);
        Mockito.verify(container, Mockito.never()).persistConfiguration();
        Assertions.assertEquals("connected-user", configured.getUserName());
        Assertions.assertEquals("new-password", configured.getUserPassword());
        Assertions.assertEquals("new-password", actual.getUserPassword());
        Mockito.verify(platformUI, Mockito.never()).showError(Mockito.any(), Mockito.any(), Mockito.any(Throwable.class));
    }

    @Test
    public void previouslyUnsavedCredentialsAreSerializedWithActualUserAndExistingAuthProperties() throws Exception {
        configured.setAuthProperty("existing-property", "preserved");
        DataSourceDescriptor descriptor = createDescriptor();
        Assertions.assertFalse(descriptor.isSavePassword());

        Assertions.assertTrue(DBAuthUtils.promptAndChangePasswordForCurrentUser(monitor, descriptor, manager));

        ArgumentCaptor<DBSSecretValue> secret = ArgumentCaptor.forClass(DBSSecretValue.class);
        Mockito.verify(secretController).setPrivateSecretValue(Mockito.eq(descriptor), secret.capture());
        Map<String, Object> saved = JSONUtils.parseMap(DBInfoUtils.SECRET_GSON, new StringReader(secret.getValue().getValue()));
        Assertions.assertEquals("test-project/datasources/test-connection", secret.getValue().getId());
        Assertions.assertEquals("connected-user", saved.get("user"));
        Assertions.assertEquals("new-password", saved.get("password"));
        Assertions.assertEquals(Map.of("existing-property", "preserved"), saved.get("properties"));
        Mockito.verify(descriptor.getRegistry(), Mockito.never()).updateDataSource(Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void cancellationLeavesDatabaseAndCredentialsUnchanged() throws DBException {
        Mockito.when(platformUI.promptUserPasswordChange(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean()
        )).thenReturn(null);
        Assertions.assertFalse(changePassword());
        Mockito.verifyNoInteractions(manager);
        Mockito.verify(container, Mockito.never()).persistSecrets(Mockito.any());
        Assertions.assertEquals("configured-password", configured.getUserPassword());
        Assertions.assertEquals("connected-password", actual.getUserPassword());
    }

    @Test
    public void rejectedPasswordDoesNotUpdateCredentials() throws DBException {
        DBException failure = new DBException("Password policy rejected the change");
        Mockito.doThrow(failure).when(manager).changeUserPassword(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertFalse(changePassword());
        Mockito.verify(container, Mockito.never()).persistSecrets(Mockito.any());
        Mockito.verify(platformUI, Mockito.never()).confirmAction(Mockito.any(), Mockito.any());
        Assertions.assertEquals("configured-password", configured.getUserPassword());
        Assertions.assertEquals("connected-password", actual.getUserPassword());
    }

    @Test
    public void decliningSaveStillUsesNewPasswordAndReportsDatabaseSuccess() throws DBException {
        Mockito.when(platformUI.confirmAction(Mockito.any(), Mockito.any())).thenReturn(false);
        Assertions.assertTrue(changePassword());
        Assertions.assertEquals("new-password", actual.getUserPassword());
        Assertions.assertEquals("configured-password", configured.getUserPassword());
        Mockito.verify(container, Mockito.never()).setSavePassword(Mockito.anyBoolean());
        Mockito.verify(container, Mockito.never()).persistSecrets(Mockito.any());
        Mockito.verify(container, Mockito.never()).persistConfiguration();
    }

    @Test
    public void storageFailureDoesNotRepeatOrReportFailedDatabaseChange() throws DBException {
        DBException failure = new DBException("Secret storage unavailable");
        Mockito.doThrow(failure).when(container).persistSecrets(secretController);
        Assertions.assertTrue(changePassword());
        Mockito.verify(manager).changeUserPassword(monitor, "connected-user", "new-password", "entered-old-password");
        Mockito.verify(platformUI).showError(
            ModelMessages.dialog_user_password_change_label, ModelMessages.dialog_user_password_change_save_error_message, failure);
        Assertions.assertEquals("new-password", actual.getUserPassword());
    }

    @Test
    public void unsupportedSecretWritesAreReportedAsSaveFailure() throws DBException {
        Mockito.when(secretController.getSupportedFeatures()).thenReturn(DBSSecretController.FEATURE_PRIVATE_SECRETS_VIEW);
        Assertions.assertTrue(changePassword());
        Mockito.verify(container, Mockito.never()).persistSecrets(Mockito.any());
        Mockito.verify(platformUI).showError(
            Mockito.eq(ModelMessages.dialog_user_password_change_label),
            Mockito.eq(ModelMessages.dialog_user_password_change_save_error_message), Mockito.any(DBException.class));
    }

    @Test
    public void temporaryConnectionUsesNewPasswordWithoutSaving() throws DBException {
        Mockito.when(container.isTemporary()).thenReturn(true);
        Assertions.assertTrue(changePassword());
        Assertions.assertEquals("new-password", actual.getUserPassword());
        Mockito.verify(platformUI, Mockito.never()).confirmAction(Mockito.any(), Mockito.any());
        Mockito.verify(container, Mockito.never()).persistSecrets(Mockito.any());
    }

    @Test
    public void savingRestrictionAllowsPasswordChangeWithoutPersisting() throws DBException {
        Assertions.assertTrue(DBAuthUtils.promptAndChangePasswordForCurrentUser(monitor, container, manager, false));
        Assertions.assertEquals("new-password", actual.getUserPassword());
        Assertions.assertEquals("configured-password", configured.getUserPassword());
        Mockito.verify(platformUI, Mockito.never()).confirmAction(Mockito.any(), Mockito.any());
        Mockito.verify(container, Mockito.never()).setSavePassword(Mockito.anyBoolean());
        Mockito.verify(container, Mockito.never()).persistSecrets(Mockito.any());
        Mockito.verify(container, Mockito.never()).persistConfiguration();
    }

    @Test
    public void localConnectionKeepsConfigurationPersistence() throws DBException {
        workbench.when(DBWorkbench::isDistributed).thenReturn(false);
        Mockito.when(project.isUseSecretStorage()).thenReturn(false);
        Mockito.when(container.persistConfiguration()).thenReturn(true);
        Assertions.assertTrue(changePassword());
        Mockito.verify(container).persistConfiguration();
        Mockito.verify(container, Mockito.never()).persistSecrets(Mockito.any());
    }

    @Test
    public void failedLocalSaveStillReportsDatabaseSuccess() throws DBException {
        workbench.when(DBWorkbench::isDistributed).thenReturn(false);
        Mockito.when(project.isUseSecretStorage()).thenReturn(false);
        Assertions.assertTrue(changePassword());
        Mockito.verify(platformUI).showError(
            Mockito.eq(ModelMessages.dialog_user_password_change_label),
            Mockito.eq(ModelMessages.dialog_user_password_change_save_error_message), Mockito.any(DBException.class));
    }

    @Test
    public void headlessApplicationDoesNotPromptOrChangePassword() {
        Mockito.when(application.isHeadlessMode()).thenReturn(true);
        Assertions.assertFalse(changePassword());
        Mockito.verifyNoInteractions(platformUI, manager);
    }

    @Test
    public void currentCredentialsFallBackOnlyWhenActualCredentialsAreMissing() {
        Assertions.assertEquals("connected-user", DBAuthUtils.getCurrentUserName(container));
        Assertions.assertEquals("connected-password", DBAuthUtils.getCurrentUserPassword(container));
        actual.setUserName(null);
        actual.setUserPassword(null);
        Assertions.assertEquals("configured-user", DBAuthUtils.getCurrentUserName(container));
        Assertions.assertEquals("configured-password", DBAuthUtils.getCurrentUserPassword(container));
    }

    @Test
    public void standaloneSecureStorageKeepsConfigurationPersistenceAndConfiguredUserName() throws DBException {
        workbench.when(DBWorkbench::isDistributed).thenReturn(false);
        Mockito.when(container.persistConfiguration()).thenReturn(true);
        configured.setUserName("${env:DB_USER}");

        Assertions.assertTrue(changePassword());

        Mockito.verify(container).persistConfiguration();
        Mockito.verify(container, Mockito.never()).persistSecrets(Mockito.any());
        Mockito.verify(container).setSavePassword(true);
        InOrder saveOrder = Mockito.inOrder(container, secretController);
        saveOrder.verify(container).persistConfiguration();
        saveOrder.verify(secretController).flushChanges();
        Assertions.assertEquals("${env:DB_USER}", configured.getUserName());
    }

    @Test
    public void standaloneSecretFlushFailureReportsSaveFailureAfterDatabaseSuccess() throws DBException {
        workbench.when(DBWorkbench::isDistributed).thenReturn(false);
        Mockito.when(container.persistConfiguration()).thenReturn(true);
        DBException failure = new DBException("Secure storage could not be flushed");
        Mockito.doThrow(failure).when(secretController).flushChanges();

        Assertions.assertTrue(changePassword());

        Mockito.verify(platformUI).showError(
            ModelMessages.dialog_user_password_change_label, ModelMessages.dialog_user_password_change_save_error_message, failure);
        Assertions.assertEquals("new-password", actual.getUserPassword());
    }

    private boolean changePassword() {
        return DBAuthUtils.promptAndChangePasswordForCurrentUser(monitor, container, manager);
    }

    private DataSourceDescriptor createDescriptor() {
        Mockito.when(platform.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        DBPDataSourceRegistry registry = Mockito.mock(DBPDataSourceRegistry.class);
        Mockito.when(registry.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        Mockito.when(registry.getProject()).thenReturn(project);
        Mockito.when(project.getId()).thenReturn("s_test-project");
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        Mockito.when(driver.getDefaultConnectionProperties()).thenReturn(Map.of());
        Mockito.when(driver.getFullId()).thenReturn("test-driver");
        DataSourceDescriptor descriptor = Mockito.spy(new DataSourceDescriptor(
            registry, Mockito.mock(DBPDataSourceConfigurationStorage.class), DataSourceOriginLocal.INSTANCE,
            "test-connection", driver, configured));
        Mockito.doReturn(actual).when(descriptor).getActualConnectionConfiguration();
        return descriptor;
    }
}
