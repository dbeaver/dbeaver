/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

/**
 * Object sink
 */
public interface WMIObjectSink {

    void indicate(WMIObject[] objects);

    void setStatus(WMIObjectSinkStatus status, int result, String param, WMIObject errorObject);
    
}
