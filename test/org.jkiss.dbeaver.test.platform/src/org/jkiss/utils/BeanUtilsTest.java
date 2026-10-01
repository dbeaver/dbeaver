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
package org.jkiss.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

public class BeanUtilsTest {

    @Test
    public void testIsGetterName() {
        Assertions.assertFalse(BeanUtils.isGetterName(""));
        Assertions.assertFalse(BeanUtils.isGetterName("foo"));

        Assertions.assertTrue(BeanUtils.isGetterName("is"));
        Assertions.assertTrue(BeanUtils.isGetterName("get"));
        Assertions.assertTrue(BeanUtils.isGetterName("has"));
    }

    @Test
    public void testGetPropertyNameFromGetter() {
        Assertions.assertNull(BeanUtils.getPropertyNameFromGetter("foobar"));

        Assertions.assertEquals("bar",
                BeanUtils.getPropertyNameFromGetter("isbar"));
        Assertions.assertEquals("bar",
                BeanUtils.getPropertyNameFromGetter("getbar"));
        Assertions.assertEquals("bar",
                BeanUtils.getPropertyNameFromGetter("hasbar"));
    }

    @Test
    public void testGetSetterName() {
        Assertions.assertNull(BeanUtils.getSetterName("foobar"));

        Assertions.assertEquals("setbar", BeanUtils.getSetterName("isbar"));
        Assertions.assertEquals("setbar", BeanUtils.getSetterName("getbar"));
        Assertions.assertEquals("setbar", BeanUtils.getSetterName("hasbar"));
    }

    @Test
    public void testGetSetMethod() {
        Assertions.assertNull(BeanUtils.getSetMethod(String.class, "size"));
        Assertions.assertNull(BeanUtils.getSetMethod(String.class, "length"));
        Assertions.assertNull(
                BeanUtils.getSetMethod(String.class, "length", true));
        Assertions.assertNull(
                BeanUtils.getSetMethod(String.class, "length", false));
    }

    @Test
    public void testGetGetMethod() {
        Assertions.assertNull(BeanUtils.getGetMethod(String.class, "size"));
        Assertions.assertNull(BeanUtils.getGetMethod(String.class, "length"));
        Assertions.assertNull(
                BeanUtils.getGetMethod(String.class, "length", true));
        Assertions.assertNull(
                BeanUtils.getGetMethod(String.class, "length", false));
    }

    @Test
    public void testPropertyNameToMethodName() {
        Assertions.assertEquals("Length",
                BeanUtils.propertyNameToMethodName("length"));
        Assertions.assertEquals("Length",
                BeanUtils.propertyNameToMethodName("Length"));
        Assertions.assertEquals("LENGTH",
                BeanUtils.propertyNameToMethodName("lENGTH"));
        Assertions.assertEquals("LENGTH",
                BeanUtils.propertyNameToMethodName("LENGTH"));
    }

    @Test
    public void testMethodNameToPropertyName() {
        Assertions.assertNull(BeanUtils.methodNameToPropertyName(""));

        Assertions.assertEquals("g", BeanUtils.methodNameToPropertyName("G"));
        Assertions.assertEquals("get", BeanUtils.methodNameToPropertyName("Get"));
        Assertions.assertEquals("empty",
                BeanUtils.methodNameToPropertyName("isEmpty"));
        Assertions.assertEquals("length",
                BeanUtils.methodNameToPropertyName("getlength"));
        Assertions.assertEquals("length",
                BeanUtils.methodNameToPropertyName("setlength"));
    }

    @Test
    public void testIsArrayType() {
        Assertions.assertFalse(BeanUtils.isArrayType(String.class));
    }

    @Test
    public void testIsCollectionType() {
        Assertions.assertFalse(BeanUtils.isCollectionType(String.class));
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

        Assertions.assertEquals(String.class,
                BeanUtils.getCollectionType(parameterizedType));

        Assertions.assertNull(BeanUtils.getCollectionType(null));
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

        Assertions.assertEquals(String.class,
                BeanUtils.getCollectionType(parameterizedType));
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

        Assertions.assertEquals(String.class,
                BeanUtils.getCollectionType(parameterizedType));
    }

    @Test
    public void testReadObjectProperty()
            throws InvocationTargetException, IllegalAccessException {
        Assertions.assertNull(BeanUtils.readObjectProperty(null, ".length"));
        Assertions.assertNull(BeanUtils.readObjectProperty(String.class, "bar"));
        Assertions.assertNull(BeanUtils.readObjectProperty(String.class, ".length"));

        Assertions.assertEquals(String.class,
                BeanUtils.readObjectProperty(String.class, "."));
    }

    @Test
    public void testIsBooleanType() {
        Assertions.assertTrue(BeanUtils.isBooleanType(Boolean.TYPE));
        Assertions.assertTrue(BeanUtils.isBooleanType(Boolean.class));

        Assertions.assertFalse(BeanUtils.isBooleanType(String.class));
        Assertions.assertFalse(BeanUtils.isBooleanType(null));
    }

    @Test
    public void testGetDefaultPrimitiveValue() {
        Assertions.assertEquals(0L,
                BeanUtils.getDefaultPrimitiveValue(Long.TYPE));
        Assertions.assertEquals(0,
                BeanUtils.getDefaultPrimitiveValue(Integer.TYPE));
        Assertions.assertEquals(0.0f,
                BeanUtils.getDefaultPrimitiveValue(Float.TYPE));
        Assertions.assertEquals(0.0,
                BeanUtils.getDefaultPrimitiveValue(Double.TYPE));
        Assertions.assertEquals((short) 0,
                BeanUtils.getDefaultPrimitiveValue(Short.TYPE));
        Assertions.assertEquals((byte) 0,
                BeanUtils.getDefaultPrimitiveValue(Byte.TYPE));
        Assertions.assertEquals((char) 0,
                BeanUtils.getDefaultPrimitiveValue(Character.TYPE));
        Assertions.assertEquals(false,
                BeanUtils.getDefaultPrimitiveValue(Boolean.TYPE));

        Assertions.assertThrows(IllegalArgumentException.class, () -> BeanUtils.getDefaultPrimitiveValue(String.class));
    }

    @Test
    public void testIsNumericType() {
        Assertions.assertTrue(BeanUtils.isNumericType(Long.TYPE));
        Assertions.assertTrue(BeanUtils.isNumericType(Byte.TYPE));
        Assertions.assertTrue(BeanUtils.isNumericType(Short.TYPE));
        Assertions.assertTrue(BeanUtils.isNumericType(Float.TYPE));
        Assertions.assertTrue(BeanUtils.isNumericType(Double.TYPE));
        Assertions.assertTrue(BeanUtils.isNumericType(Short.class));
        Assertions.assertTrue(BeanUtils.isNumericType(Integer.TYPE));

        Assertions.assertFalse(BeanUtils.isNumericType(String.class));
    }

    @Test
    public void testInvokeObjectMethod() throws Throwable {
        Assertions.assertEquals("0", BeanUtils.invokeObjectMethod("String",
                "valueOf", new Class<?>[]{int.class}, new Object[]{0}));
        Assertions.assertEquals(6,
                BeanUtils.invokeObjectMethod("String", "length"));
    }

    @Test
    @SuppressWarnings("unused")
    public void testInvokeObjectDeclaredMethod() throws Throwable {
        class DummyClass {
            private int getValueA() {
                return 123;
            }

            protected int getValueB() {
                return 456;
            }

            protected int getValueC() {
                return 789;
            }
        }

        class DummyChild extends DummyClass {
            @Override
            protected int getValueC() {
                return 0;
            }
        }

        final DummyChild child = new DummyChild();

        Assertions.assertEquals(123, BeanUtils.invokeObjectDeclaredMethod(
            child,
            "getValueA",
            new Class[0],
            new Object[0]
        ));

        Assertions.assertEquals(456, BeanUtils.invokeObjectDeclaredMethod(
            child,
            "getValueB",
            new Class[0],
            new Object[0]
        ));

        Assertions.assertEquals(0, BeanUtils.invokeObjectDeclaredMethod(
            child,
            "getValueC",
            new Class[0],
            new Object[0]
        ));

        Assertions.assertThrows(NoSuchMethodException.class, () -> BeanUtils.invokeObjectDeclaredMethod(
            child,
            "getValueD",
            new Class[0],
            new Object[0]
        ));
    }

    @Test
    public void testInvokeStaticMethod() throws Throwable {
        Assertions.assertEquals("0", BeanUtils.invokeStaticMethod(String.class,
                "valueOf", new Class<?>[]{int.class}, new Object[]{0}));
    }
}
