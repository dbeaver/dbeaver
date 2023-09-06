/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.internal.core.StreamsProxy;
import org.jkiss.dbeaver.debug.core.DebugUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DatabaseProcess implements IProcess {

    private final ILaunch launch;
    private final String name;
    private final Map<String, String> attributes = new HashMap<>();

    private boolean terminated = false;
    private Process process ;


    public DatabaseProcess(ILaunch launch, String name) {
        this.launch = launch;
        this.name = name;
        launch.addProcess(this);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public boolean canTerminate() {
        return !terminated;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public void terminate() throws DebugException {
        if (!terminated) {
            terminated = true;
            process.destroy();
            launch.removeProcess(this);
            DebugUtils.fireTerminate(this);
        }
    }

    @Override
    public String getLabel() {
        return name;
    }

    @Override
    public ILaunch getLaunch() {
        return launch;
    }

    @Override
    public IStreamsProxy getStreamsProxy() {
//        return null;
    	//return new StreamsProxy(this, UTF_8, "yangmeng test--> ");
        try {
            final ByteArrayInputStream stdout = new ByteArrayInputStream("你好, YashanDB Console...".getBytes(StandardCharsets.UTF_8));
            process = new MockProcess(stdout, null, 0);
            final StreamsProxy streamProxy = new StreamsProxy(process, "UTF_8");

            //TODO:YANGMENG
            //new Thread("console"){
            //    @Override
            //    public void run() {
            //        for (int i = 0; i < 10; i++) {
            //            try {
            //                Thread.sleep(2000);
            //                outputStream.write(("yangmeng test console -"+i).getBytes());
            //                outputStream.flush();
            //            } catch (Exception e) {
            //                e.printStackTrace();
            //                throw new RuntimeException(e);
            //            }finally {
            //                try {
            //                    outputStream.close();
            //                } catch (IOException e) {
            //                    throw new RuntimeException(e);
            //                }
            //            }
            //        }
            //    }
            //}.start();
            return streamProxy;


            //Process process = Runtime.getRuntime().exec("cmd /k date");
            //OutputStream outputStream = process.getOutputStream();
            //new Thread("console"){
            //    @Override
            //    public void run() {
            //        for (int i = 0; i < 10; i++) {
            //            try {
            //                Thread.sleep(2000);
            //                outputStream.write(("yangmeng test console -"+i).getBytes());
            //                outputStream.flush();
            //            } catch (Exception e) {
            //                e.printStackTrace();
            //                throw new RuntimeException(e);
            //            }
            //        }
            //    }
            //}.start();
            //return new StreamsProxy(process, StandardCharsets.UTF_8, "yangmeng test console--> ");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    @Override
    public String getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public int getExitValue() throws DebugException {
        return 0;
    }

}
