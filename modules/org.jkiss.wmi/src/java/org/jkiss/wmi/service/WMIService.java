/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import org.apache.commons.logging.Log;

/**
 * WMI Service
 * Uses native Win32 API access
 */
public class WMIService {

    private long serviceHandle = 0l;
    private Log serviceLog;

    public WMIService(Log serviceLog) {
        this.serviceLog = serviceLog;
    }

    public native void connect(String domain, String host, String user, String password, String locale, String resource)
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
