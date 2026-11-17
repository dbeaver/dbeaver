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
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

public interface DDSharedProjectEventSource {
    @NotNull
    DDRemoteDamSubscription subscribe(@NotNull Subscriber subscriber);

    interface Subscriber {

        void start() throws DBException;

        void stop() throws DBException;

        default void onEvent(@NotNull DDSharedProjectEvent event) throws DBException {
            switch (event.change()) {
                case CREATED -> projectCreated(event);
                case METADATA_UPDATED -> projectMetadataUpdated(event);
                case CONFIGURATION_UPDATED -> projectConfigurationUpdated(event);
                case DELETED -> projectDeleted(event);
                default -> throw new IllegalStateException("Unexpected shared project change: " + event.change());
            }
        }

        void projectCreated(@NotNull DDSharedProjectEvent event) throws DBException;

        void projectMetadataUpdated(@NotNull DDSharedProjectEvent event) throws DBException;

        void projectConfigurationUpdated(@NotNull DDSharedProjectEvent event) throws DBException;

        void projectDeleted(@NotNull DDSharedProjectEvent event) throws DBException;
    }
}
