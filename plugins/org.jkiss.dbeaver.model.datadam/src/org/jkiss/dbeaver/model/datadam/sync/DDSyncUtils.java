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
package org.jkiss.dbeaver.model.datadam.sync;

import org.jkiss.code.NotNull;

public final class DDSyncUtils {

    private DDSyncUtils() {
    }

    @NotNull
    public static DDSyncChange classify(
        @NotNull String localFingerprint,
        @NotNull String lastKnownRevisionFingerprint,
        @NotNull String serverFingerprint
    ) {
        boolean localChanged = !lastKnownRevisionFingerprint.equals(localFingerprint);
        boolean serverChanged = !lastKnownRevisionFingerprint.equals(serverFingerprint);
        if (localFingerprint.equals(serverFingerprint)) {
            return serverChanged ? DDSyncChange.SERVER : DDSyncChange.UNCHANGED;
        }
        if (localChanged && serverChanged) {
            return DDSyncChange.CONFLICT;
        }
        return localChanged ? DDSyncChange.LOCAL : DDSyncChange.SERVER;
    }
}
