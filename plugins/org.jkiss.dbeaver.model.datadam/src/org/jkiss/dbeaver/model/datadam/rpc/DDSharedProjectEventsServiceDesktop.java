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
import org.jkiss.dbeaver.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DDSharedProjectEventsServiceDesktop implements DDSharedProjectEventsService, DDSharedProjectEventSource {
    private static final Log log = Log.getLog(DDSharedProjectEventsServiceDesktop.class);
    private static final DDSharedProjectEventsServiceDesktop INSTANCE = new DDSharedProjectEventsServiceDesktop();

    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private boolean started;

    private DDSharedProjectEventsServiceDesktop() {
    }

    @NotNull
    public static DDSharedProjectEventsServiceDesktop getInstance() {
        return INSTANCE;
    }

    @NotNull
    @Override
    public synchronized DDRemoteDamSubscription subscribe(@NotNull Subscriber subscriber) {
        subscribers.add(subscriber);
        if (started) {
            startSubscriber(subscriber);
        }
        return () -> unsubscribe(subscriber);
    }

    private synchronized void unsubscribe(@NotNull Subscriber subscriber) {
        if (subscribers.remove(subscriber)) {
            stopSubscriber(subscriber);
        }
    }

    @Override
    public void projectChanged(@NotNull DDSharedProjectEvent event) {
        log.debug("Received shared project event " + event.change() + " for project " + event.projectId());
        for (Subscriber subscriber : subscribers) {
            try {
                subscriber.onEvent(event);
            } catch (Exception e) {
                log.warn("Shared project event listener failed", e);
            }
        }
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        for (Subscriber subscriber : subscribers) {
            startSubscriber(subscriber);
        }
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }
        started = false;
        for (Subscriber subscriber : subscribers) {
            stopSubscriber(subscriber);
        }
    }

    private void startSubscriber(@NotNull Subscriber subscriber) {
        try {
            subscriber.start();
        } catch (Exception e) {
            log.warn("Could not start shared project subscriber", e);
        }
    }

    private void stopSubscriber(@NotNull Subscriber subscriber) {
        try {
            subscriber.stop();
        } catch (Exception e) {
            log.warn("Could not stop shared project subscriber", e);
        }
    }
}
