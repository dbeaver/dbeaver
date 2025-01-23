/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.junit.osgi.delegate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.runner.notification.RunListener;

import java.io.*;

public class ClassTransferHandler {

    /**
     * Transfer object to another classloader
     * @param value object to transfer
     * @param targetClassloader target classloader
     * @return transferred object or delegated object or null if transfer is not possible or failed
     */
    public static Object transfer(Object value, ClassLoader targetClassloader) {
        if (value.getClass().getClassLoader().equals(targetClassloader)) {
            return value;
        }
        try {
            if (value instanceof Serializable serializable) {
                return deserialize(serialize(serializable), targetClassloader);
            } else if (value instanceof RunListener) {
                Class<?> delegateClass = targetClassloader.loadClass(
                    "org.jkiss.junit.osgi.delegate.RunListenerDelegate");
                return delegateClass
                    .getConstructor(Object.class)
                    .newInstance(value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error transferring class", e);
        }
        return null;
    }
    private static byte[] serialize(Serializable object) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new ObjectOutputStream(buffer).writeObject(object);
        return buffer.toByteArray();
    }

    private static Object deserialize(byte[] data, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        ByteArrayInputStream buffer = new ByteArrayInputStream(data);
        return new ObjectInputStream(buffer) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
                return Class.forName(desc.getName(), false, classLoader);
            }
        }.readObject();
    }


}
