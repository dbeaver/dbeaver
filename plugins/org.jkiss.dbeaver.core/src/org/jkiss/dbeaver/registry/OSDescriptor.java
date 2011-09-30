/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    @Override
    public String toString() {
        return code + (arch == null ? "" : " (" + arch + ")");
    }
}
