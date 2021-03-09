/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.runtime;

/**
 * Operating system descriptor
 */
public class OSDescriptor {
    private final String family;
    private final String arch;

    public OSDescriptor(String family, String arch) {
        this.family = family;
        this.arch = arch;
    }

    public boolean matches(OSDescriptor os) {
        if (!family.equals(os.family)) {
            return false;
        }
        return arch == null || (os.arch != null && arch.equals(os.arch));
    }
}
