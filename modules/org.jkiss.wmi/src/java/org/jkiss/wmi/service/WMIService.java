/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
