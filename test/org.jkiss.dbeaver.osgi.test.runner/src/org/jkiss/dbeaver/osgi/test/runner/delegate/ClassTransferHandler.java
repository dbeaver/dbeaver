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
package org.jkiss.dbeaver.osgi.test.runner.delegate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.runner.notification.RunListener;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

public class ClassTransferHandler {
    static Gson gson = new GsonBuilder().create();
    public static Object transfer(Object value, ClassLoader targetClassloader) {
        if (value.getClass().getClassLoader().equals(targetClassloader)) {
            return value;
        }
        try {
            if (value instanceof Serializable serializable) {
                return deserialize(serialize(value), targetClassloader, serializable.getClass().getName());
            } else if (value instanceof RunListener) {
                Class<?> delegateClass = targetClassloader.loadClass(
                    "org.jkiss.dbeaver.osgi.test.runner.delegate.RunListenerDelegate");
                return delegateClass
                    .getConstructor(Object.class)
                    .newInstance(value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error transferring class", e);
        }
        return null;
    }
    private static String serialize(Object description)  {
        return gson.toJson(description);
    }

    private static Object deserialize(String data, ClassLoader classLoader, String classname) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Object gson = classLoader.loadClass(ClassTransferHandler.gson.getClass().getName()).getConstructor().newInstance();
        Object o = gson.getClass().getMethod("fromJson").invoke(gson, data, classLoader.loadClass(classname));

        return o;
    }


}
