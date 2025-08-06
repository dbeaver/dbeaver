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
package org.jkiss.dbeaver.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import org.jkiss.dbeaver.model.meta.SecureProperty;

public class PropertySerializationUtils {
    public static String EMPTY_JSON_OBJECT = "{}";

    private static class SecurePropertiesExclusionStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            return fieldAttributes.getAnnotation(SecureProperty.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
            return false;
        }
    }

    private static class NonSecurePropertiesExclusionStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            return fieldAttributes.getAnnotation(SecureProperty.class) == null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
            return false;
        }
    }

    /**
     * @return GsonBuilder which will exclude secured properties
     */
    public static GsonBuilder baseNonSecurePropertiesGsonBuilder() {
        return new GsonBuilder().setExclusionStrategies(new SecurePropertiesExclusionStrategy())
            .setPrettyPrinting()
            .setStrictness(Strictness.LENIENT);

    }

    /**
     * @return GsonBuilder which will include only secured properties
     */
    public static GsonBuilder baseSecurePropertiesGsonBuilder() {
        return new GsonBuilder().setExclusionStrategies(new NonSecurePropertiesExclusionStrategy())
            .setPrettyPrinting()
            .setStrictness(Strictness.LENIENT);
    }
}
