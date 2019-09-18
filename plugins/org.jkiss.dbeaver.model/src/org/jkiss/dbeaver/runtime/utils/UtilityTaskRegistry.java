/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.utils;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;

public class UtilityTaskRegistry
{
    private static final Log log = Log.getLog(UtilityTaskRegistry.class);

    private static UtilityTaskRegistry instance = null;

    public synchronized static UtilityTaskRegistry getInstance()
    {
        if (instance == null) {
            instance = new UtilityTaskRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private UtilityTaskRegistry(IExtensionRegistry registry)
    {
    }

}
