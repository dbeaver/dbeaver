/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 * Copyright (C) 2010-2019 Eric Hettiaratchi (erichettiaratchi@gmail.com)
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
package org.jkiss.utils;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

public class TypeUtilsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testIsArrayType() {
        Assert.assertFalse(TypeUtils.isArrayType(String.class));
    }

    @Test
    public void testIsCollectionType() {
        Assert.assertFalse(TypeUtils.isCollectionType(String.class));
    }

    @Test
    public void testGetCollectionType1() {
        ParameterizedType parameterizedType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{String.class};
            }

            @Override
            public Type getRawType() {
                return null;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        Assert.assertEquals(String.class,
                TypeUtils.getCollectionType(parameterizedType));

        Assert.assertNull(TypeUtils.getCollectionType(null));
    }

    @Test
    public void testGetCollectionType2() {
        WildcardType wildcardType = new WildcardType() {
            @Override
            public Type[] getUpperBounds() {
                return new Type[]{String.class, Integer.class};
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };

        ParameterizedType parameterizedType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{wildcardType};
            }

            @Override
            public Type getRawType() {
                return null;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        Assert.assertEquals(String.class,
                TypeUtils.getCollectionType(parameterizedType));
    }

    @Test
    public void testGetCollectionType3() {
        WildcardType wildcardType = new WildcardType() {
            @Override
            public Type[] getUpperBounds() {
                return new Type[0];
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[]{String.class, Integer.class};
            }
        };

        ParameterizedType parameterizedType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{wildcardType};
            }

            @Override
            public Type getRawType() {
                return null;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        Assert.assertEquals(String.class,
                TypeUtils.getCollectionType(parameterizedType));
    }

    @Test
    public void testIsBooleanType() {
        Assert.assertTrue(TypeUtils.isBooleanType(Boolean.TYPE));
        Assert.assertTrue(TypeUtils.isBooleanType(Boolean.class));

        Assert.assertFalse(TypeUtils.isBooleanType(String.class));
        Assert.assertFalse(TypeUtils.isBooleanType(null));
    }

    @Test
    public void testGetDefaultPrimitiveValue() {
        Assert.assertEquals(0L,
                TypeUtils.getDefaultPrimitiveValue(Long.TYPE));
        Assert.assertEquals(0,
                TypeUtils.getDefaultPrimitiveValue(Integer.TYPE));
        Assert.assertEquals(0.0f,
                TypeUtils.getDefaultPrimitiveValue(Float.TYPE));
        Assert.assertEquals(0.0,
                TypeUtils.getDefaultPrimitiveValue(Double.TYPE));
        Assert.assertEquals((short) 0,
                TypeUtils.getDefaultPrimitiveValue(Short.TYPE));
        Assert.assertEquals((byte) 0,
                TypeUtils.getDefaultPrimitiveValue(Byte.TYPE));
        Assert.assertEquals((char) 0,
                TypeUtils.getDefaultPrimitiveValue(Character.TYPE));
        Assert.assertEquals(false,
                TypeUtils.getDefaultPrimitiveValue(Boolean.TYPE));

        thrown.expect(IllegalArgumentException.class);
        TypeUtils.getDefaultPrimitiveValue(String.class);
    }

    @Test
    public void testIsNumericType() {
        Assert.assertTrue(TypeUtils.isNumericType(Long.TYPE));
        Assert.assertTrue(TypeUtils.isNumericType(Byte.TYPE));
        Assert.assertTrue(TypeUtils.isNumericType(Short.TYPE));
        Assert.assertTrue(TypeUtils.isNumericType(Float.TYPE));
        Assert.assertTrue(TypeUtils.isNumericType(Double.TYPE));
        Assert.assertTrue(TypeUtils.isNumericType(Short.class));
        Assert.assertTrue(TypeUtils.isNumericType(Integer.TYPE));

        Assert.assertFalse(TypeUtils.isNumericType(String.class));
    }
}
