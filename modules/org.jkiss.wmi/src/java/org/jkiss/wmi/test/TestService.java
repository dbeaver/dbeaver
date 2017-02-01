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
package org.jkiss.wmi.test;

import org.jkiss.wmi.service.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WMI Service tester
 */
public class TestService {

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
        try {
            {
                Thread testThread = new Thread() {
                    @Override
                    public void run()
                    {
                        try {
                            service = WMIService.connect(null, "localhost", null, null, null, "root");
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
        System.exit(0);
    }

    private void testNamespace()
        throws WMIException
    {
        //WMIService.initializeThread();

        ObjectCollectorSink classesSink;

        nsService = service.openNamespace("cimv2");
        ObjectCollectorSink tmpSink = new ObjectCollectorSink();
        nsService.executeQuery("SELECT * FROM Win32_Process", tmpSink, WMIConstants.WBEM_FLAG_SEND_STATUS);
        tmpSink.waitForFinish();
        for (WMIObject o : tmpSink.objectList) {
            System.out.println("=============");
//            for (WMIObjectAttribute attr : o.getAttributes(WMIConstants.WBEM_FLAG_ALWAYS)) {
//                System.out.println(attr.toString());
//            }
            System.out.println("Caption=" + o.getValue("Caption"));
            System.out.println("CommandLine=" + o.getValue("CommandLine"));
            System.out.println("CreationClassName=" + o.getValue("CreationClassName"));
            System.out.println("CreationDate=" + o.getValue("CreationDate"));
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
            this.objectList = new ArrayList<>();
        }

        @Override
        public void indicate(WMIObject[] objects)
        {
            Collections.addAll(objectList, objects);
        }

        @Override
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
