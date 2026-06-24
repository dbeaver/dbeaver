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
package org.jkiss.dbeaver.model.impl.config;

import org.jkiss.utils.CommonUtils;

public final class ProductConfigUtils {
    /**
     * Force Product Config to show at every launch
     */
    private static final String PROP_SHOW_ON_STARTUP = "dbeaver.show.easy.config.on.startup";

    private ProductConfigUtils() {
    }

    /**
     * Returns whether Product Config should be shown at startup.
     * <p>
     * This is determined by the system property {@code dbeaver.show.easy.config.on.startup}.
     */
    public static boolean isShowOnStartup() {
        return CommonUtils.getBoolean(System.getProperty(PROP_SHOW_ON_STARTUP));
    }
}
