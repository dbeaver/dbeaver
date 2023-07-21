/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.rm.file;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A simple file visitor with default behavior to visit all files and folders
 * and perform the same operation on them and to re-throw I/O errors.
 */
@FunctionalInterface
public interface UniversalFileVisitor<T> extends FileVisitor<T> {

    /**
     * Method that applies to both files and folders starting with {@code dirOrFile}
     */
    FileVisitResult dirOrFileOperation(T dirOrFile, BasicFileAttributes attrs) throws IOException;

    @Override
    default FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs) throws IOException {
        dirOrFileOperation(dir, attrs);
        return FileVisitResult.CONTINUE;
    }

    @Override
    default FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws IOException {
        dirOrFileOperation(file, attrs);
        return FileVisitResult.CONTINUE;
    }

    @Override
    default FileVisitResult visitFileFailed(T file, IOException exc) throws IOException {
        throw exc;
    }

    @Override
    default FileVisitResult postVisitDirectory(T dir, IOException exc) throws IOException {
        if (exc != null) {
            throw exc;
        }
        return FileVisitResult.CONTINUE;
    }
}
