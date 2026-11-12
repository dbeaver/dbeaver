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
package org.jkiss.dbeaver.ext.mimer.model.data;

import org.jkiss.code.NotNull;

/**
 * Implemented by any {@code DBDContentStorage} that can report a bounded preview of a CLOB/NCLOB
 * value cheaply, without materializing or re-reading the rest of it - {@link
 * MimerChunkedCharsStorage} and {@link MimerSpilledCharsStorage} both qualify. {@link
 * MimerContentCLOB#getDisplayString} checks for this rather than either concrete type.
 *
 * @author Mimer Information Technology
 */
interface MimerPreviewableStorage {

    @NotNull
    String peekPreview(int maxLength);
}
