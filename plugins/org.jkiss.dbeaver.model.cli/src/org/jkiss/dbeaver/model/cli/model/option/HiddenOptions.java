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
package org.jkiss.dbeaver.model.cli.model.option;

import picocli.CommandLine;

/**
 * Eclipse options, we do not process them,
 * properties hidden and exists to avoid unmatched options error
 */
public class HiddenOptions {
    public static final String PRODUCT_OPTION = "-product";

    @CommandLine.Option(names = {"-web-config"}, arity = "1", hidden = true)
    private String webConfig;

    @CommandLine.Option(names = {"-cli-mode"}, hidden = true)
    private boolean cliMode;

    @CommandLine.Option(names = {"-consoleLog"}, hidden = true)
    private boolean consoleLog;

    @CommandLine.Option(names = {"-registryMultiLanguage"}, hidden = true)
    private boolean registryMultiLanguage;

    @CommandLine.Option(names = {PRODUCT_OPTION}, arity = "1", hidden = true)
    private String product;

    @CommandLine.Option(names = {"-dev"}, arity = "1", hidden = true)
    private String dev;

    @CommandLine.Option(names = {"-os"}, arity = "1", hidden = true)
    private String os;

    @CommandLine.Option(names = {"-ws"}, arity = "1", hidden = true)
    private String ws;

    @CommandLine.Option(names = {"-arch"}, arity = "1", hidden = true)
    private String arch;

    @CommandLine.Option(names = {"-eclipse.keyring"}, arity = "1", hidden = true)
    private String eclipseKeyring;

    @CommandLine.Option(names = {"-launcher"}, arity = "1", hidden = true)
    private String launcher;
}
