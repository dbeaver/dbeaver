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

import com.dbeaver.datadam.dam.remote.api.model.DDSharedProjectEvent;
import com.dbeaver.datadam.dam.remote.api.service.desktop.DDSharedProjectEventsService;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DDSharedProjectEventsServiceDesktop implements DDSharedProjectEventsService, DDSharedProjectEventSource {
    private static final Log log = Log.getLog(DDSharedProjectEventsServiceDesktop.class);
    private static final DDSharedProjectEventsServiceDesktop INSTANCE = new DDSharedProjectEventsServiceDesktop();

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private DDSharedProjectEventsServiceDesktop() {
    }

    @NotNull
    public static DDSharedProjectEventsServiceDesktop getInstance() {
        return INSTANCE;
    }

    @NotNull
    @Override
    public DDRemoteDamSubscription subscribe(@NotNull Subscriber subscriber) {
        Listener registration = new Listener(subscriber);
        listeners.add(registration);
        return () -> listeners.remove(registration);
    }

    @Override
    public void projectChanged(@NotNull DDSharedProjectEvent event) {
        for (Listener listener : listeners) {
            try {
                listener.subscriber.onEvent(event);
            } catch (DBException | RuntimeException e) {
                log.warn("Shared project event listener failed", e);
            }
        }
    }

    private static class Listener {
        private final Subscriber subscriber;

        private Listener(@NotNull Subscriber subscriber) {
            this.subscriber = subscriber;
        }
    }
}
