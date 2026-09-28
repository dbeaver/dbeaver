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

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.utils.ArrayUtils;

/**
 * Remote DataDam endpoints used for login, project synchronization and tracking.
 */
public final class DDEndpoints {
    private static final String STAGE_ARGUMENT = "-dbeaver.datadam.stage";

    private static final String STAGE_ACCOUNT_URL = "https://account.datadam.beavers.team";
    private static final String STAGE_STORAGE_URL = "https://gateway.datadam.beavers.team";
    //TODO Replace these placeholders with the production endpoints once they are available.
    private static final String PROD_ACCOUNT_URL = "https://account.prod.datadam.invalid";
    private static final String PROD_STORAGE_URL = "https://storage.prod.datadam.invalid";

    private static final boolean STAGE = ArrayUtils.contains(Platform.getApplicationArgs(), STAGE_ARGUMENT);

    private DDEndpoints() {
    }

    @NotNull
    public static String getAccountBaseUrl() {
        return STAGE ? STAGE_ACCOUNT_URL : PROD_ACCOUNT_URL;
    }

    @NotNull
    public static String getStorageBaseUrl() {
        return STAGE ? STAGE_STORAGE_URL : PROD_STORAGE_URL;
    }
}
