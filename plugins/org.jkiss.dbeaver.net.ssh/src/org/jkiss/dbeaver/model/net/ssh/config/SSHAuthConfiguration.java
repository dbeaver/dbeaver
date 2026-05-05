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
package org.jkiss.dbeaver.model.net.ssh.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public sealed interface SSHAuthConfiguration {
    sealed interface WithPassword extends SSHAuthConfiguration {
        @Nullable
        String password();

        boolean savePassword();
    }

    record Password(@Nullable String password, boolean savePassword) implements WithPassword {
    }

    record KeyFile(@NotNull String path, @Nullable String password, boolean savePassword) implements WithPassword {
    }

    record KeyData(@NotNull String data, @Nullable String password, boolean savePassword) implements WithPassword {
    }

    record Agent() implements SSHAuthConfiguration {
    }
}
