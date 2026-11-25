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
package org.jkiss.dbeaver.model.datadam.auth.desktop;

import com.dbeaver.datadam.dam.remote.api.model.DDGatewayKeyMaterial;
import com.dbeaver.datadam.dam.remote.api.model.DDGatewayKeyRegistrationStatus;
import com.dbeaver.datadam.dam.remote.api.service.web.DDGatewayKeyService;
import com.dbeaver.datadam.sso.api.DDPermissionIds;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import com.dbeaver.messaging.rpc.MessagingRpcException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.keys.DDGatewayKeyMaterialGenerator;
import org.jkiss.dbeaver.model.datadam.auth.desktop.keys.DDRecoveryPhraseGenerator;
import org.jkiss.dbeaver.model.datadam.rpc.DBDamSessionService;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;

class DBDamEncryptionKeyGenerationTest {
    private static final String PHRASE =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
    private static final DDGatewayKeyMaterial MATERIAL = new DDGatewayKeyMaterial(
        "public-key", "encrypted-bundle", "salt", 600_000, "PBKDF2-HMAC-SHA256");

    @Test
    void returnsThePhraseAfterConfirmedRegistrationWithoutDownloadingTheBundle() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Mockito.when(fixture.remote.registerKey(MATERIAL)).thenReturn(DDGatewayKeyRegistrationStatus.REGISTERED);

            Assertions.assertEquals(PHRASE, fixture.service.generateNewKey(fixture.monitor));
            Mockito.verify(fixture.phrases).generate();
            Mockito.verify(fixture.materials).generate(PHRASE);
            Mockito.verify(fixture.remote).registerKey(MATERIAL);
            Mockito.verifyNoInteractions(fixture.access);
            Mockito.verify(fixture.monitor, Mockito.times(DBDamEncryptionKeyService.GENERATION_WORK_UNITS)).worked(1);
        }
    }

    @Test
    void verifiesTheBundleWhenTheRegistrationReplyIsLost() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Mockito.when(fixture.remote.registerKey(MATERIAL))
                .thenThrow(new MessagingRpcException(MessagingRpcException.Reason.TIMEOUT));
            Mockito.when(fixture.access.prepareKeyImport(PHRASE)).thenReturn(fixture.verified);

            Assertions.assertEquals(PHRASE, fixture.service.generateNewKey(fixture.monitor));
            Mockito.verify(fixture.access).prepareKeyImport(PHRASE);
            Mockito.verify(fixture.access, Mockito.never()).importKeys(Mockito.anyString());
        }
    }

    @Test
    void verifiesTheBundleWhenRpcIsUnavailableBeforeRegistration() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Mockito.when(fixture.session.getRpc()).thenReturn(fixture.rpc)
                .thenThrow(new DBException("RPC channel unavailable"));
            Mockito.when(fixture.access.prepareKeyImport(PHRASE)).thenReturn(fixture.verified);

            Assertions.assertEquals(PHRASE, fixture.service.generateNewKey(fixture.monitor));
            Mockito.verify(fixture.remote, Mockito.never()).registerKey(MATERIAL);
            Mockito.verify(fixture.access).prepareKeyImport(PHRASE);
        }
    }

    @Test
    void rejectsKnownRegistrationResultsWithoutDownloadingTheBundle() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Mockito.when(fixture.remote.registerKey(MATERIAL)).thenReturn(
                DDGatewayKeyRegistrationStatus.ALREADY_EXISTS,
                DDGatewayKeyRegistrationStatus.INVALID_MATERIAL,
                DDGatewayKeyRegistrationStatus.NOT_ALLOWED);

            Assertions.assertTrue(Assertions.assertThrows(DBException.class, () -> fixture.service.generateNewKey(fixture.monitor))
                .getMessage().contains("already exist"));
            Assertions.assertTrue(Assertions.assertThrows(DBException.class, () -> fixture.service.generateNewKey(fixture.monitor))
                .getMessage().contains("Invalid"));
            Assertions.assertTrue(Assertions.assertThrows(DBException.class, () -> fixture.service.generateNewKey(fixture.monitor))
                .getMessage().contains("cannot generate"));

            Mockito.verifyNoInteractions(fixture.access);
        }
    }

    @Test
    void keepsRegistrationFailureWhenVerificationAlsoFailsWithoutRetainingTheAttempt() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Mockito.when(fixture.remote.registerKey(MATERIAL))
                .thenThrow(new MessagingRpcException(MessagingRpcException.Reason.TIMEOUT))
                .thenReturn(DDGatewayKeyRegistrationStatus.REGISTERED);
            Mockito.when(fixture.access.prepareKeyImport(PHRASE))
                .thenThrow(new DBException("The encrypted bundle is not available yet"));

            DBException error = Assertions.assertThrows(DBException.class, () -> fixture.service.generateNewKey(fixture.monitor));
            Assertions.assertInstanceOf(MessagingRpcException.class, error.getCause());
            Assertions.assertEquals(1, error.getCause().getSuppressed().length);
            Assertions.assertEquals(PHRASE, fixture.service.generateNewKey(fixture.monitor));

            Mockito.verify(fixture.phrases, Mockito.times(2)).generate();
            Mockito.verify(fixture.materials, Mockito.times(2)).generate(PHRASE);
            Mockito.verify(fixture.access).prepareKeyImport(PHRASE);
        }
    }

    @Test
    void honorsMonitorCancellationBeforeSendingAndAllowsAnotherAttempt() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Mockito.doAnswer(invocation -> {
                Mockito.when(fixture.monitor.isCanceled()).thenReturn(true);
                return MATERIAL;
            }).when(fixture.materials).generate(PHRASE);

            Assertions.assertTrue(Assertions.assertThrows(InterruptedException.class,
                () -> fixture.service.generateNewKey(fixture.monitor))
                .getMessage().contains("canceled"));
            Mockito.verify(fixture.remote, Mockito.never()).registerKey(MATERIAL);
            Mockito.verifyNoInteractions(fixture.access);

            Mockito.doReturn(MATERIAL).when(fixture.materials).generate(PHRASE);
            Mockito.when(fixture.monitor.isCanceled()).thenReturn(false);
            Mockito.when(fixture.remote.registerKey(MATERIAL)).thenReturn(DDGatewayKeyRegistrationStatus.REGISTERED);
            Assertions.assertEquals(PHRASE, fixture.service.generateNewKey(fixture.monitor));
        }
    }

    @Test
    void completesRegistrationDespiteLateMonitorCancellation() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Mockito.when(fixture.remote.registerKey(MATERIAL)).thenAnswer(invocation -> {
                Mockito.when(fixture.monitor.isCanceled()).thenReturn(true);
                return DDGatewayKeyRegistrationStatus.REGISTERED;
            });

            Assertions.assertEquals(PHRASE, fixture.service.generateNewKey(fixture.monitor));
            Mockito.verifyNoInteractions(fixture.access);
        }
    }

    @Test
    void skipsGenerationWhenMonitorWasAlreadyCanceled() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Mockito.when(fixture.monitor.isCanceled()).thenReturn(true);

            Assertions.assertThrows(InterruptedException.class, () -> fixture.service.generateNewKey(fixture.monitor));
            Mockito.verifyNoInteractions(fixture.phrases, fixture.materials, fixture.remote, fixture.access);
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final UUID accountId = UUID.randomUUID();
        private final DDAccountClient account = Mockito.mock(DDAccountClient.class);
        private final DBDamAccessService access = Mockito.mock(DBDamAccessService.class);
        private final DDRecoveryPhraseGenerator phrases = Mockito.mock(DDRecoveryPhraseGenerator.class);
        private final DDGatewayKeyMaterialGenerator materials = Mockito.mock(DDGatewayKeyMaterialGenerator.class);
        private final DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        private final DBDamSessionService session = Mockito.mock(DBDamSessionService.class);
        private final MessagingRpcEndpoint rpc = Mockito.mock(MessagingRpcEndpoint.class);
        private final DDGatewayKeyService remote = Mockito.mock(DDGatewayKeyService.class);
        private final DDKeyBundle verified = new DDKeyBundle(accountId, "signing", "data", 1, UUID.randomUUID());
        private final MockedStatic<DBDamSessionService> sessions = Mockito.mockStatic(DBDamSessionService.class);
        private final DBDamEncryptionKeyService service = new DBDamEncryptionKeyService(account, access, phrases, materials);

        private Fixture() throws Exception {
            sessions.when(DBDamSessionService::getInstance).thenReturn(session);
            Mockito.when(session.getRpc()).thenReturn(rpc);
            Mockito.when(rpc.proxy(DDGatewayKeyService.SERVICE_ID, DDGatewayKeyService.class)).thenReturn(remote);
            Mockito.when(account.getAccountId()).thenReturn(accountId);
            Mockito.when(account.hasPermission(DDPermissionIds.STORAGE_KEY_MANAGE)).thenReturn(true);
            Mockito.when(remote.isKeysPresent()).thenReturn(false);
            Mockito.when(phrases.generate()).thenReturn(PHRASE);
            Mockito.when(materials.generate(PHRASE)).thenReturn(MATERIAL);
        }

        @Override
        public void close() {
            sessions.close();
        }
    }
}
