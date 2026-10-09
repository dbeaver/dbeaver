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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.jkiss.code.NotNull;

import java.util.Arrays;

final class AIImageClipboardTransfer extends ByteArrayTransfer {
    static final AIImageClipboardTransfer INSTANCE = new AIImageClipboardTransfer();
    private static final String[] TYPE_NAMES = {"public.png", "image/png", "PNG"};
    private static final int[] TYPE_IDS = Arrays.stream(TYPE_NAMES).mapToInt(ByteArrayTransfer::registerType).toArray();

    private AIImageClipboardTransfer() {
    }

    @NotNull
    @Override
    protected String[] getTypeNames() {
        return TYPE_NAMES;
    }

    @NotNull
    @Override
    protected int[] getTypeIds() {
        return TYPE_IDS;
    }
}
