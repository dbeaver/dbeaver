/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.wmi.service.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * WMI Service tester
 */
public class TestService {

    final static Log log = LogFactory.getLog(TestService.class);

    private WMIService service;
    private boolean finished = false;
    private WMIService nsService;

    public TestService()
    {
    }

    public static void main(String[] args)
    {
        new TestService()
            .test();
    }

    void test()
    {
        log.info("Start service");
        try {
            //service.connect("bq", "aelita", "jurgen", "CityMan78&", null, "\\root\\cimv2");
            {
                Thread testThread = new Thread() {
                    @Override
                    public void run()
                    {
                        try {
                            service = WMIService.connect(log, null, "localhost", null, null, null, "root");
                            ObjectCollectorSink classesSink = new ObjectCollectorSink();
                            service.enumClasses(null, classesSink, 0);
                            classesSink.waitForFinish();
                            Thread.sleep(10000);

                        } catch (WMIException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                };
                testThread.start();
                Thread.sleep(1000);
            }

            {
                Thread testThread2 = new Thread() {
                    @Override
                    public void run()
                    {
                        try {
                            testNamespace();
                        } catch (WMIException e) {
                            e.printStackTrace();
                        }
                    }
                };
                testThread2.start();
                testThread2.join();
            }

        } catch (InterruptedException e) {
            // do nothing
        } finally {
            service.close();
        }

        System.gc();
        System.out.println("DONE");
    }

    private void testNamespace()
        throws WMIException
    {
        //WMIService.initializeThread();

        ObjectCollectorSink classesSink;

        nsService = service.openNamespace("cimv2");
        ObjectCollectorSink tmpSink = new ObjectCollectorSink();
        nsService.executeQuery("SELECT * FROM Win32_Service", tmpSink, WMIConstants.WBEM_FLAG_SEND_STATUS);
        tmpSink.waitForFinish();
        for (WMIObject o : tmpSink.objectList) {
            System.out.println(o.getValue("Name"));
        }

        classesSink = new ObjectCollectorSink();
        nsService.enumClasses(null, classesSink, 0);
        classesSink.waitForFinish();
        for (WMIObject classDesc : classesSink.objectList) {
            final Collection<WMIObjectMethod> methods = classDesc.getMethods(WMIConstants.WBEM_FLAG_ALWAYS);
            if (methods != null) {

            }
        }

        ObjectCollectorSink objectCollectorSink = new ObjectCollectorSink();
/*
            service.executeQuery(
                //"select * from Win32_NTLogEvent",
                "select * from Win32_Group",
                objectCollectorSink,
                WMIConstants.WBEM_FLAG_SEND_STATUS
            );
*/
        nsService.enumInstances("Win32_Group", objectCollectorSink, WMIConstants.WBEM_FLAG_SEND_STATUS);
        objectCollectorSink.waitForFinish();

        for (WMIObject nsDesc : objectCollectorSink.objectList) {
            System.out.println(nsDesc.getValue("Name"));
            final Collection<WMIQualifier> qfList = nsDesc.getQualifiers();
            if (qfList != null) {

            }

//                final Object nsName = nsDesc.getValue("Name");
//                final WMIService nsService = service.openNamespace(nsName.toString());
//
//                ObjectCollectorSink classCollectorSink = new ObjectCollectorSink();
//                nsService.enumClasses(null, classCollectorSink, WMIConstants.WBEM_FLAG_SEND_STATUS | WMIConstants.WBEM_FLAG_SHALLOW);
//                classCollectorSink.waitForFinish();
//                nsService.close();
        }
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

        for (WMIObjectAttribute prop : object.getAttributes(WMIConstants.WBEM_FLAG_ALWAYS)) {
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

    private class ObjectCollectorSink implements WMIObjectSink
    {
        private final List<WMIObject> objectList;
        private boolean finished = false;

        public ObjectCollectorSink()
        {
            this.objectList = new ArrayList<WMIObject>();
        }

        public void indicate(WMIObject[] objects)
        {
            Collections.addAll(objectList, objects);
        }

        public void setStatus(WMIObjectSinkStatus status, int result, String param, WMIObject errorObject)
        {
            if (status == WMIObjectSinkStatus.complete) {
                finished = true;
            }
        }

        public void waitForFinish()
        {
            try {
                while (!finished) {
                    Thread.sleep(100);
                }
                //service.cancelAsyncOperation(wmiObjectSink);
            }
            catch (InterruptedException e) {
                // do nothing
            }
        }
    }
}
