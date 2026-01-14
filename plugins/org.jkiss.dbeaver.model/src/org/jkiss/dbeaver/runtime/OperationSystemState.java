/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.runtime;

import org.jkiss.dbeaver.Log;

import java.awt.*;
import java.awt.desktop.SystemSleepEvent;
import java.awt.desktop.SystemSleepListener;

public class OperationSystemState {
    private static final Log log = Log.getLog(OperationSystemState.class);

    private static long sleepStart;

    public static boolean isInSleepMode() {
        return sleepStart > 0;
    }

    public static long getSleepStartTime() {
        return sleepStart;
    }

    public static void toggleSleepMode(boolean sleep) {
        log.debug(sleep ? "Application goes into sleep mode" : "Application wakes");
        OperationSystemState.sleepStart = sleep ? System.currentTimeMillis() : 0;
    }

}
