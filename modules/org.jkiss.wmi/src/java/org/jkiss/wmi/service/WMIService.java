/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * WMI Service
 * Uses native Win32 API access
 */
public class WMIService {

    private long serviceHandle;
    private Log serviceLog;

    public WMIService(Log serviceLog) {
        this.serviceHandle = 0;
        this.serviceLog = serviceLog;
    }

    public native void connect(String domain, String host, String user, String password, String locale, String resource)
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
            System.load(new File("jkiss_wmi_x86.dll").getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Can not link to DLL.", e);
        }

        serviceLog.info("Start service");
        try {
            //this.connect("bq", "aelita", "jurgen", "CityMan78&", null, "\\root\\cimv2");
            this.connect(null, "localhost", null, null, null, "\\root\\cimv2");
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
                    for (WMIObject object : objects) {
                        try {
                            examineObject(object);
                            //object.release();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
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
                //"select * from Win32_NTLogEvent",
                "select * from __Namespace",
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
        try {
            System.out.println("====== " + object.getObjectText());
        } catch (WMIException e) {
            e.printStackTrace();
        }
    }

    private static void examineObject(WMIObject object) throws WMIException
    {
        final String objectText = object.getObjectText();
        //final Object name = object.getValue("Name");

        for (WMIObjectProperty prop : object.getProperties()) {
            Object propValue = prop.getValue();
            if (propValue instanceof Object[]) {
                //System.out.print("\t" + prop.getName() + "= { ");
                Object[] array = (Object[])propValue;
                for (int i = 0; i < array.length; i++) {
                    //if (i > 0) System.out.print(", ");
                    //System.out.print("'" + array[i] + "'");
                }
                //System.out.println(" }");
            } else if (propValue instanceof byte[]) {
                //System.out.println("\t" + prop.getName() + "= { byte array } " + ((byte[])propValue).length);
            } else {
                //System.out.println("\t" + prop.getName() + "=" + propValue);
            }
        }
    }
}
