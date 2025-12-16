/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone.cli;

import picocli.CommandLine;

import java.util.List;

public class DBeaverMixin {
    @CommandLine.Option(
        names = {"-followReferences"},
        hidden = true
    )
    private boolean followReferences;

    @CommandLine.Option(
        names = {"-repository"},
        arity = "1",
        hidden = true
    )
    private List<String> repositories;


    @CommandLine.Option(
        names = {"-installIU"},
        arity = "1",
        hidden = true
    )
    private String installIU;

    @CommandLine.Option(
        names = {"-application"},
        arity = "1",
        hidden = true
    )
    private String application;
}
