/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.websocket.WSConstants;

public class WSTransactionalCountEvent extends WSAbstractEvent {

    private final String projectId;
    private final String contextId;
    private final int transactionalCount;

    public WSTransactionalCountEvent(String projectId, String contextId, int transactionalCount) {
        super("cb_transactional_count", WSConstants.TOPIC_TRANSACTION_COUNT);
        this.transactionalCount = transactionalCount;
        this.projectId = projectId;
        this.contextId = contextId;
    }
}
