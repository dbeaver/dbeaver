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
package org.jkiss.wmi.service;

/**
 * WMI Service
 * Uses native Win32 API access
 */
public class WMIService {

//    static {
//        String arch = System.getProperty("os.arch");
//        if (arch != null && arch.indexOf("64") != -1) {
//            System.loadLibrary("jkiss_wmi_x86_64");
//        } else {
//            System.loadLibrary("jkiss_wmi_x86");
//        }
//    }

    public static void linkNative(String libPath)
    {
        System.load(libPath);
    }

    private long serviceHandle = 0l;

    public static native WMIService connect(String domain, String host, String user, String password, String locale, String resource)
        throws WMIException;

    public native WMIService openNamespace(String namespace)
        throws WMIException;

    public native void executeQuery(String query, WMIObjectSink sink, long flags)
        throws WMIException;

    public native void enumClasses(String superClass, WMIObjectSink sink, long flags)
        throws WMIException;

    public native void enumInstances(String className, WMIObjectSink sink, long flags)
        throws WMIException;

    public native void cancelSink(WMIObjectSink sink)
        throws WMIException;

    public native void close();

}
