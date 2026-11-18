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

import com.dbeaver.datadam.dam.remote.api.DDRemoteDamSettingKeys;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSettings;
import com.dbeaver.datadam.dam.remote.api.service.desktop.DDSharedProjectEventsService;
import com.dbeaver.messaging.requestreply.MessagingSubscription;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import com.dbeaver.messaging.rpc.MessagingRpcException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public class DDSharedProjectDesktopRpcRegistration implements DDDesktopRpcRegistration {
    private final DDSharedProjectEventsServiceDesktop service = DDSharedProjectEventsServiceDesktop.getInstance();
    @Nullable
    private MessagingRpcEndpoint registeredEndpoint;
    @Nullable
    private MessagingSubscription subscription;

    @NotNull
    @Override
    public String getId() {
        return DDSharedProjectEventsService.SERVICE_ID;
    }

    @Override
    public synchronized void register(@NotNull MessagingRpcEndpoint endpoint, @NotNull DDRemoteDamSettings settings) {
        if (endpoint.isClosed()) {
            throw new MessagingRpcException(MessagingRpcException.Reason.CLOSED);
        }
        if (registeredEndpoint != endpoint) {
            unregister();
            registeredEndpoint = endpoint;
        }
        if (settings.isDamEnabled(DDRemoteDamSettingKeys.PROJECTS_STREAM_SYNC, false)) {
            if (subscription == null) {
                subscription = endpoint.register(DDSharedProjectEventsService.SERVICE_ID, DDSharedProjectEventsService.class, service);
                service.start();
            }
        } else {
            unregister();
        }
    }

    private void unregister() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
            service.stop();
        }
    }
}
