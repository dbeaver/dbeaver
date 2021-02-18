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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class SimpleByteArrayTransfer extends ByteArrayTransfer {
    private static final String FORMAT_NAME = "BinaryMemoryByteArrayTypeName";
    private static final int FORMAT_ID = registerType(FORMAT_NAME);

    private static final SimpleByteArrayTransfer instance = new SimpleByteArrayTransfer();

    private SimpleByteArrayTransfer() {
    }

    public static SimpleByteArrayTransfer getInstance() {
        return instance;
    }

    @Override
    public void javaToNative(Object object, TransferData transferData) {
        if (object == null || !(object instanceof byte[])) return;

        if (isSupportedType(transferData)) {
            byte[] buffer = (byte[]) object;
            super.javaToNative(buffer, transferData);
        }
    }

    @Override
    public Object nativeToJava(TransferData transferData) {
        Object result = null;
        if (isSupportedType(transferData)) {
            result = super.nativeToJava(transferData);
        }

        return result;
    }

    @Override
    protected String[] getTypeNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    protected int[] getTypeIds() {
        return new int[]{FORMAT_ID};
    }

}
