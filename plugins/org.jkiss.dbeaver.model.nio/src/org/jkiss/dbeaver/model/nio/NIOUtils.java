/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.nio;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public class NIOUtils {
    @NotNull
    public static String resolve(@NotNull String separator, @Nullable String basePath, @NotNull String path) {
        if (basePath == null) {
            return path;
        }
        return basePath + separator + path;
    }
}
