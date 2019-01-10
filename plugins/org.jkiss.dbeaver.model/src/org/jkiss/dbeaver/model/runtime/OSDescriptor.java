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

package org.jkiss.dbeaver.model.runtime;

/**
 * OS (Operational System) descriptor
 */
public class OSDescriptor {

    private String code;
    private String arch;

    public OSDescriptor(String code, String arch) {
        this.code = code;
        this.arch = arch;
    }

    public String getCode() {
        return code;
    }

    public String getArch() {
        return arch;
    }

    public boolean matches(OSDescriptor os)
    {
        if (!code.equals(os.code)) {
            return false;
        }
        if (arch != null && (os.arch == null || !arch.equals(os.arch))) {
            return false;
        }
        // The same OS
        return true;
    }

    public boolean isWindows()
    {
        return "win32".equals(code);
    }

    @Override
    public String toString() {
        return code + (arch == null ? "" : " (" + arch + ")");
    }

    public boolean is64()
    {
        return "x86_64".equals(arch);
    }
}
