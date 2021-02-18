/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.exec.compile;

/**
 * Compile error
 */
public class DBCCompileError {

    private boolean error;
    private String message;
    private int line;
    private int position;

    public DBCCompileError(boolean error, String message, int line, int position)
    {
        this.error = error;
        this.message = message;
        this.line = line;
        this.position = position;
    }

    public boolean isError()
    {
        return error;
    }

    public String getMessage()
    {
        return message;
    }

    public int getLine()
    {
        return line;
    }

    public int getPosition()
    {
        return position;
    }

    @Override
    public String toString()
    {
        if (line <= 0) {
            return message;
        } else {
            return message + "\nCompile error at line " + line + ", column " + position;
        }
    }
}
