/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
        return code.equals("win32");
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
