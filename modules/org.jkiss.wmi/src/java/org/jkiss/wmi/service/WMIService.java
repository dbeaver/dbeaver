/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

/**
 * WMI Service
 * Uses native Win32 API access
 */
public class WMIService {

    static {
        String arch = System.getProperty("os.arch");
        if (arch != null && arch.indexOf("64") != -1) {
            System.loadLibrary("jkiss_wmi_x86_64");
        } else {
            System.loadLibrary("jkiss_wmi_x86");
        }
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
