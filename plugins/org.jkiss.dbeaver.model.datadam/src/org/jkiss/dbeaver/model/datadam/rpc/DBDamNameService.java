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
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Supplies the desktop name sent with the initial Dam registration. */
public final class DBDamNameService {
    private DBDamNameService() {
    }

    /** Initial naming is now performed by registration; retained for callers that read the registered Dam. */
    @Deprecated
    @Nullable
    public static DDConnectedRemoteDam getOrInitializeName(@NotNull DDAccountClient account, @NotNull String clientDamId)
        throws DBException {
        return account.getDam(clientDamId);
    }

    @NotNull
    public static String suggestDamName() {
        String computer = System.getenv("COMPUTERNAME");
        if (computer == null || computer.isBlank()) {
            computer = System.getenv("HOSTNAME");
        }
        if (computer == null || computer.isBlank()) {
            try {
                computer = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                // The product name is still a usable suggestion without a computer name.
            }
        }
        return formatName(GeneralUtils.getProductName(), computer);
    }

    @NotNull
    static String formatName(@NotNull String product, @Nullable String computer) {
        return computer == null || computer.isBlank() ? product : product + ": " + computer;
    }
}
