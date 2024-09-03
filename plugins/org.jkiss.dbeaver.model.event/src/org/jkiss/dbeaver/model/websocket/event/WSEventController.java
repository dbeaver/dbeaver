/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.websocket.event;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.websocket.WSEventHandler;
import org.jkiss.dbeaver.model.websocket.registry.WSEventHandlersRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WSEventController {
    private static final Log log = Log.getLog(WSEventController.class);

    private final Map<String, List<WSEventHandler>> eventHandlersByType = new HashMap<>();
    protected final List<WSEvent> eventsPool = new ArrayList<>();
    private boolean forceSkipEvents = false;

    public WSEventController() {

        var eventHandlerDescriptors = WSEventHandlersRegistry.getInstance().readDescriptors();

        eventHandlerDescriptors.forEach(descriptor -> {
            var handler = descriptor.getInstance();
            descriptor.getSupportedTopics().forEach(
                topic -> eventHandlersByType.computeIfAbsent(topic, x -> new ArrayList<>()).add(handler)
            );
        });
    }

    /**
     * Add cb event to the event pool
     */
    public void addEvent(@NotNull WSEvent event) {
        if (!event.isForceProcessed() && forceSkipEvents) {
            return;
        }
        synchronized (eventsPool) {
            eventsPool.add(event);
        }
    }

    /**
     * Add cb event to the event pool
     */
    public void scheduleCheckJob() {
        new CBEventCheckJob().schedule();
    }

    /**
     * Flag to check if we need to skip events
     */
    public void setForceSkipEvents(boolean forceSkipEvents) {
        this.forceSkipEvents = forceSkipEvents;
    }


    private class CBEventCheckJob extends AbstractJob {
        private static final long CHECK_PERIOD = 1000;

        protected CBEventCheckJob() {
            super("CloudBeaver events job");
            setUser(false);
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            List<WSEvent> events;

            synchronized (eventsPool) {
                events = List.copyOf(eventsPool);
                eventsPool.clear();
            }
            if (events.isEmpty()) {
                schedule(CHECK_PERIOD);
                return Status.OK_STATUS;
            }
            for (WSEvent event : events) {
                eventHandlersByType.getOrDefault(event.getTopicId(), List.of()).forEach(handler -> {
                    try {
                        handler.handleEvent(event);
                    } catch (Exception e) {
                        log.error(
                            "Error on event handle " + event.getTopicId(),
                            e
                        );
                    }
                });
            }
            schedule(CHECK_PERIOD);
            return Status.OK_STATUS;
        }
    }
}
