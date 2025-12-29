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
package org.jkiss.dbeaver.model.cli;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.cli.model.option.InputFileOption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CLIUtils {
    private static final Log log = Log.getLog(CLIUtils.class);

    @Nullable
    public static String readValueFromFileOrSystemIn(@Nullable InputFileOption filesOptions) throws CLIException {
        String value = null;
        if (filesOptions == null || filesOptions.getInputFile() == null) {
            value = tryReadFromSystemIn();
        } else if (filesOptions.getInputFile() != null) {
            if (Files.notExists(filesOptions.getInputFile())) {
                throw new CLIException(
                    "Input file does not exist: " + filesOptions.getInputFile(),
                    CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
                );
            }
            try {
                value = Files.readString(filesOptions.getInputFile());
            } catch (IOException e) {
                throw new CLIException(
                    "Error reading GQL from input file: " + filesOptions.getInputFile(),
                    e,
                    CLIConstants.EXIT_CODE_ERROR
                );
            }
        }
        return value;
    }

    @Nullable
    private static String tryReadFromSystemIn() {
        try {
            if (System.in.available() > 0) {
                return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Error reading from system in", e);
            return null;
        }
        return null;
    }
}
