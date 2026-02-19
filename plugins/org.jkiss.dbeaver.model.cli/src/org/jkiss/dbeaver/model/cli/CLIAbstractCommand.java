/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.cli;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.cli.model.CLIInitializer;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class CLIAbstractCommand implements Callable<Void> {
    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Override
    public Void call() throws CLIException {
        initialize();
        run();
        return null;
    }

    @NotNull
    protected abstract CLIContext context();

    public abstract void run() throws CLIException;

    protected void initialize() throws CLIException {
        List<CLIInitializer> initializers = findMixins(CLIInitializer.class);
        for (CLIInitializer initializer : initializers) {
            initializer.initialize(context());
        }
    }

    @NotNull
    protected <T> List<T> findMixins(@NotNull Class<T> implClass) {
        List<T> updaters = new ArrayList<>();
        var curSpec = spec;
        while (curSpec != null) {
            if (!CommonUtils.isEmpty(curSpec.mixins())) {
                for (CommandLine.Model.CommandSpec mixin : curSpec.mixins().values()) {
                    if (mixin.userObject() == null) {
                        continue;
                    }
                    if (implClass.isAssignableFrom(mixin.userObject().getClass())) {
                        updaters.add(implClass.cast(mixin.userObject()));
                    }
                }
            }
            curSpec = curSpec.parent();
        }

        return updaters;
    }
}
