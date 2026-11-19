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
package org.jkiss.dbeaver.model.cli.model.option;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.regex.Pattern;

public class OutputFileOption extends AbstractFileOption {
    private static final Pattern URI_SCHEME_PREFIX = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:");

    @Nullable
    @CommandLine.Option(
        names = {"-out", "-output-file"},
        arity = "1",
        description = "Write the execution result to a file."
    )
    private String outputFile;

    @Nullable
    public Path getOutputFile() {
        if (!CommonUtils.isEmpty(outputFile)
            && (IOUtils.isLocalFile(outputFile) || !URI_SCHEME_PREFIX.matcher(outputFile).find())
        ) {
            String launchDirectory = System.getenv(CLIConstants.ENV_LAUNCH_DIRECTORY);
            Path path = Path.of(outputFile);
            return CommonUtils.isEmpty(launchDirectory) ? path.toAbsolutePath() : Path.of(launchDirectory).resolve(path);
        }
        return getPath(outputFile);
    }
}
