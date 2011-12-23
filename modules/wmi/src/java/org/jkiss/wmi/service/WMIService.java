/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * WMI Service
 * Uses native Win32 API access
 */
public class WMIService {

    long serviceHandle;
    Log serviceLog;

    public WMIService(Log serviceLog) {
        this.serviceHandle = 0;
        this.serviceLog = serviceLog;
    }

    public native void connect(String domain, String host, String user, String password, String locale)
        throws WMIException;

    public native WMIObject[] executeQuery(String query, boolean sync)
        throws WMIException;

    public native void executeQueryAsync(String query, WMIObjectSink sink, boolean sendStatus)
        throws WMIException;

    public native void cancelAsyncOperation(WMIObjectSink sink)
        throws WMIException;

    public native void close();

    private boolean finished = false;

    public static void main(String[] args)
    {
        new WMIService(LogFactory.getLog(WMIService.class)).test();
    }

    void test()
    {
        try {
            // load native library
            System.load("jkiss_wmi_x86.dll");
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Can not link to DLL.", e);
        }

        serviceLog.info("Start service");
        try {
            this.connect("bq", "aelita", "jurgen", "CityMan78#", null);
            //service.connect(null, "localhost", null, null, null);
            final long curTime = System.currentTimeMillis();

/*
            WMIObject[] result = service.executeQuery(
                "select * from Win32_NTLogEvent where LogFile='System'", false);
            System.out.println("Exec time: " + (System.currentTimeMillis() - curTime) + "ms");

            for (WMIObject object : result) {
                printObject(object);
            }
*/


            WMIObjectSink wmiObjectSink = new WMIObjectSink() {
                private int totalObjects = 0;

                public void indicate(WMIObject[] objects) {
                    totalObjects += objects.length;
                    System.out.println("Recieved " + objects.length + " (" + totalObjects + ") objects");
                }

                public void setStatus(WMIObjectSinkStatus status, int progress, String param, WMIObject errorObject) {
                    System.out.println("Status: " + status + "; " + Integer.toHexString(progress) + "; " + param);
                    if (errorObject != null) {
                        printObject(errorObject);
                    }
                    System.out.println("Total objects: " + totalObjects);
                    System.out.println("Exec time: " + (System.currentTimeMillis() - curTime) + "ms");
                    finished = true;
                }
            };
            this.executeQueryAsync(
                "select * from Win32_NTLogEvent where LogFile='Security'",
                wmiObjectSink,
                true
            );
            try {
                while (!finished) {
                    Thread.sleep(1000);
                }
                //service.cancelAsyncOperation(wmiObjectSink);
            }
            catch (InterruptedException e) {
                // do nothing
            }
/*
            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                // do nothing
            }
*/

        } catch (WMIException e) {
            e.printStackTrace();
        }
        finally {
            this.close();
        }

        System.gc();
        System.out.println("DONE");
    }

    private static void printObject(WMIObject object)
    {
        System.out.println("====== " + object);

        for (String propName : object.getProperties().keySet()) {
            Object propValue = object.getProperty(propName);
            if (propValue instanceof Object[]) {
                System.out.print("\t" + propName + "= { ");
                Object[] array = (Object[])propValue;
                for (int i = 0; i < array.length; i++) {
                    if (i > 0) System.out.print(", ");
                    System.out.print("'" + array[i] + "'");
                }
                System.out.println(" }");
            } else if (propValue instanceof byte[]) {
                System.out.println("\t" + propName + "= { byte array } " + ((byte[])propValue).length);
            } else {
                System.out.println("\t" + propName + "=" + propValue);
            }
        }
    }
}
