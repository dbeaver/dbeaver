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
package org.jkiss.dbeaver.model.datadam.sync.project;

import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.sync.DDSyncChange;

public final class DDProjectSyncUtils {

    private static final Log log = Log.getLog(DDProjectSyncUtils.class);

    private DDProjectSyncUtils() {
    }

    @NotNull
    public static DDSyncChange classify(
        @NotNull DDSharedProjectRevision lastSyncedRevision,
        @NotNull DDSharedProjectRevision serverRevision,
        @NotNull String localFingerprint
    ) {
        String lastKnownRevisionFingerprintprint = lastSyncedRevision.configurationFingerprint();
        String serverFingerprint = serverRevision.configurationFingerprint();
        DDSyncChange change = classify(localFingerprint, lastKnownRevisionFingerprintprint, serverFingerprint);
        log.debug("Classified project sync change as " + change + ": lastKnownRevision=" + lastKnownRevisionFingerprintprint +
            ", local=" + localFingerprint + ", server=" + serverFingerprint);
        return change;
    }

    @NotNull
    public static DDSyncChange classify(
        @NotNull String localFingerprint,
        @NotNull String lastKnownRevisionFingerprint,
        @NotNull String serverFingerprint
    ) {
        boolean localChanged = !lastKnownRevisionFingerprint.equals(localFingerprint);
        boolean serverChanged = !lastKnownRevisionFingerprint.equals(serverFingerprint);
        DDSyncChange change;
        if (localFingerprint.equals(serverFingerprint)) {
            //case when local and server have the same file state for some reason. Then - we must pull and up[date last sync
            change = serverChanged ? DDSyncChange.SERVER : DDSyncChange.UNCHANGED;
        } else if (localChanged && serverChanged) {
            change = DDSyncChange.CONFLICT;
        } else if (localChanged) {
            change = DDSyncChange.LOCAL;
        } else {
            change = DDSyncChange.SERVER;
        }
        return change;
    }
}
