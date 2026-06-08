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
package org.jkiss.dbeaver.ui.config.easy.nls;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.lang.reflect.Field;

public abstract class ReloadableNLS extends NLS {
    private static final Log log = Log.getLog(ReloadableNLS.class);

    public static void reloadMessages(@NotNull String name, @NotNull Class<?> clazz) {
        try {
            // OSGI caches the suffixes, so we need to reset it to reload messages
            Field field = NLS.class.getDeclaredField("nlSuffixes");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            log.error("Failed to reset NLS cache", e);
        }

        NLS.initializeMessages(name, clazz);
    }
}
