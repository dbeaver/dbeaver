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
package org.jkiss.dbeaver.model.datadam.rpc;

import com.dbeaver.datadam.dam.remote.api.model.DDConnectedRemoteDam;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;

@Timeout(5)
class DDDesktopDamNameServiceTest {
    private static final String CLIENT_ID = "desktop-client";
    private static final String DEFAULT_NAME = "DBeaver Test: workstation";
    private final DDAccountClient account = Mockito.mock(DDAccountClient.class);

    @Test
    void persistsTheGeneratedNameAndReturnsTheServerResult() throws Exception {
        var saved = dam(DEFAULT_NAME);
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(dam(null));
        Mockito.when(account.canRenameDam()).thenReturn(true);
        Mockito.when(account.renameDam(CLIENT_ID, DEFAULT_NAME)).thenReturn(saved);

        Assertions.assertSame(saved, DDDesktopDamNameService.getOrInitializeName(account, CLIENT_ID, () -> DEFAULT_NAME));

        var order = Mockito.inOrder(account);
        order.verify(account).getDam(CLIENT_ID);
        order.verify(account).canRenameDam();
        order.verify(account).renameDam(CLIENT_ID, DEFAULT_NAME);
    }

    @Test
    void preservesAnExistingServerNameWithoutGeneratingAnotherName() throws Exception {
        var existing = dam("My database workstation");
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(existing);

        Assertions.assertSame(existing, DDDesktopDamNameService.getOrInitializeName(account, CLIENT_ID,
            () -> Assertions.fail("An already named Dam must not generate a name")));

        Mockito.verify(account).getDam(CLIENT_ID);
        Mockito.verifyNoMoreInteractions(account);
    }

    @Test
    void leavesAnUnregisteredDamUntouched() throws Exception {
        Assertions.assertNull(DDDesktopDamNameService.getOrInitializeName(account, CLIENT_ID,
            () -> Assertions.fail("A name must not be generated before server registration")));
        Mockito.verify(account).getDam(CLIENT_ID);
        Mockito.verifyNoMoreInteractions(account);
    }

    @Test
    void respectsRenamePermissionWithoutShowingAnUnsavedSuggestion() throws Exception {
        var unnamed = dam(null);
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(unnamed);

        Assertions.assertSame(unnamed, DDDesktopDamNameService.getOrInitializeName(account, CLIENT_ID,
            () -> Assertions.fail("A name must not be generated without rename permission")));

        Mockito.verify(account).getDam(CLIENT_ID);
        Mockito.verify(account).canRenameDam();
        Mockito.verifyNoMoreInteractions(account);
    }

    @Test
    void reconnectDoesNotRenameAnAlreadyInitializedDam() throws Exception {
        var saved = dam(DEFAULT_NAME);
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(dam(null), saved);
        Mockito.when(account.canRenameDam()).thenReturn(true);
        Mockito.when(account.renameDam(CLIENT_ID, DEFAULT_NAME)).thenReturn(saved);

        DDDesktopDamNameService.getOrInitializeName(account, CLIENT_ID, () -> DEFAULT_NAME);
        Assertions.assertSame(saved, DDDesktopDamNameService.getOrInitializeName(account, CLIENT_ID,
            () -> Assertions.fail("Reconnect must preserve the saved name")));

        Mockito.verify(account).renameDam(CLIENT_ID, DEFAULT_NAME);
    }

    @Test
    void retriesAnUnpersistedNameAfterARequestFailure() throws Exception {
        var saved = dam(DEFAULT_NAME);
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(dam(null));
        Mockito.when(account.canRenameDam()).thenReturn(true);
        Mockito.when(account.renameDam(CLIENT_ID, DEFAULT_NAME)).thenThrow(new DBException("Unavailable")).thenReturn(saved);

        Assertions.assertThrows(DBException.class,
            () -> DDDesktopDamNameService.getOrInitializeName(account, CLIENT_ID, () -> DEFAULT_NAME));
        Assertions.assertSame(saved, DDDesktopDamNameService.getOrInitializeName(account, CLIENT_ID, () -> DEFAULT_NAME));
    }

    @NotNull
    private static DDConnectedRemoteDam dam(@Nullable String name) {
        var now = OffsetDateTime.now();
        return new DDConnectedRemoteDam("server-dam", "deployment", "instance", "workspace", null, true, 1,
            List.of(new DDConnectedRemoteDam.Application("DBeaver Test", "1.0")), now, now, null, CLIENT_ID, List.of(), name);
    }
}
