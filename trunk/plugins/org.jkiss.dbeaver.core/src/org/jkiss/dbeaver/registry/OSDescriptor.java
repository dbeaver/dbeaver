/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.registry;

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
