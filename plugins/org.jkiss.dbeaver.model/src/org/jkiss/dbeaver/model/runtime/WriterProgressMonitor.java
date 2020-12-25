/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.runtime;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * Progress monitor with extra logging
 */
public class WriterProgressMonitor extends ProxyProgressMonitor {

    private final PrintWriter out;

    public WriterProgressMonitor(DBRProgressMonitor monitor, Writer out) {
        super(monitor);
        this.out = new PrintWriter(out);
    }

    @Override
    public void beginTask(String name, int totalWork) {
        super.beginTask(name, totalWork);
        out.println(name);
    }

    @Override
    public void subTask(String name) {
        super.subTask(name);
        out.println("\t" + name);
    }
}