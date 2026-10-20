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
package org.jkiss.dbeaver.model.datadam;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.transport.DDDesktopMessageChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DDDesktopConnectionServiceTest {
    private final DDDesktopAccessService access = Mockito.mock(DDDesktopAccessService.class);
    private final DDDesktopMessageChannel channel = Mockito.mock(DDDesktopMessageChannel.class);
    private final DDDesktopConnectionService service = new DDDesktopConnectionService(access, channel);

    @AfterEach
    void close() {
        service.close();
    }

    @Test
    void startsTheChannelWithoutRequiringImportedKeys() {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);

        service.start();
        service.start();

        Mockito.verify(channel).start();
        Mockito.verify(access, Mockito.never()).getKeyBundle();
        Assertions.assertSame(channel, service.getChannel());
    }

    @ParameterizedTest
    @EnumSource(value = DDDesktopAccessService.Status.class, names = {"SIGNED_OUT", "EXPIRED", "UNLOADED"})
    void doesNotStartLoginOrTransportWhenAuthorizationIsUnavailable(@NotNull DDDesktopAccessService.Status status) {
        Mockito.when(access.getStatus()).thenReturn(status);

        service.start();

        Mockito.verify(channel).pause();
        Mockito.verify(channel, Mockito.never()).start();
    }

    @Test
    void pausesBeforeAnIdentityChangeAndResumesAfterASuccessfulLogin() {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
        service.start();
        DDDesktopAccessListener listener = listener();

        listener.accessChanging();
        Mockito.verify(channel).pause();
        listener.accessChanged();

        Mockito.verify(channel, Mockito.times(2)).start();
    }

    @Test
    void pausesOnLogoutAndCanResumeAfterANewLogin() {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
        service.start();
        DDDesktopAccessListener listener = listener();

        listener.accessChanging();
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_OUT);
        listener.accessChanged();
        Mockito.verify(channel, Mockito.times(2)).pause();
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
        listener.accessChanged();

        Mockito.verify(channel, Mockito.times(2)).start();
    }

    @Test
    void shutdownDetachesTheListenerAndCannotBeReversedByLateAccessEvents() {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
        service.start();
        DDDesktopAccessListener listener = listener();

        service.close();
        service.close();
        listener.accessChanged();
        service.start();

        Mockito.verify(channel).close();
        Mockito.verify(channel).start();
        Mockito.verify(access).removeAccessListener(listener);
    }

    @NotNull
    private DDDesktopAccessListener listener() {
        ArgumentCaptor<DDDesktopAccessListener> listener = ArgumentCaptor.forClass(DDDesktopAccessListener.class);
        Mockito.verify(access).addAccessListener(listener.capture());
        return listener.getValue();
    }
}
