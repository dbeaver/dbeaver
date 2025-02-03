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
package org.jkiss.utils;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

public class BeanUtilsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testIsGetterName() {
        Assert.assertFalse(BeanUtils.isGetterName(""));
        Assert.assertFalse(BeanUtils.isGetterName("foo"));

        Assert.assertTrue(BeanUtils.isGetterName("is"));
        Assert.assertTrue(BeanUtils.isGetterName("get"));
        Assert.assertTrue(BeanUtils.isGetterName("has"));
    }

    @Test
    public void testGetPropertyNameFromGetter() {
        Assert.assertNull(BeanUtils.getPropertyNameFromGetter("foobar"));

        Assert.assertEquals("bar",
                BeanUtils.getPropertyNameFromGetter("isbar"));
        Assert.assertEquals("bar",
                BeanUtils.getPropertyNameFromGetter("getbar"));
        Assert.assertEquals("bar",
                BeanUtils.getPropertyNameFromGetter("hasbar"));
    }

    @Test
    public void testGetSetterName() {
        Assert.assertNull(BeanUtils.getSetterName("foobar"));

        Assert.assertEquals("setbar", BeanUtils.getSetterName("isbar"));
        Assert.assertEquals("setbar", BeanUtils.getSetterName("getbar"));
        Assert.assertEquals("setbar", BeanUtils.getSetterName("hasbar"));
    }

    @Test
    public void testGetSetMethod() {
        Assert.assertNull(BeanUtils.getSetMethod(String.class, "size"));
        Assert.assertNull(BeanUtils.getSetMethod(String.class, "length"));
        Assert.assertNull(
                BeanUtils.getSetMethod(String.class, "length", true));
        Assert.assertNull(
                BeanUtils.getSetMethod(String.class, "length", false));
    }

    @Test
    public void testGetGetMethod() {
        Assert.assertNull(BeanUtils.getGetMethod(String.class, "size"));
        Assert.assertNull(BeanUtils.getGetMethod(String.class, "length"));
        Assert.assertNull(
                BeanUtils.getGetMethod(String.class, "length", true));
        Assert.assertNull(
                BeanUtils.getGetMethod(String.class, "length", false));
    }

    @Test
    public void testPropertyNameToMethodName() {
        Assert.assertEquals("Length",
                BeanUtils.propertyNameToMethodName("length"));
        Assert.assertEquals("Length",
                BeanUtils.propertyNameToMethodName("Length"));
        Assert.assertEquals("LENGTH",
                BeanUtils.propertyNameToMethodName("lENGTH"));
        Assert.assertEquals("LENGTH",
                BeanUtils.propertyNameToMethodName("LENGTH"));
    }

    @Test
    public void testMethodNameToPropertyName() {
        Assert.assertNull(BeanUtils.methodNameToPropertyName(""));

        Assert.assertEquals("g", BeanUtils.methodNameToPropertyName("G"));
        Assert.assertEquals("get", BeanUtils.methodNameToPropertyName("Get"));
        Assert.assertEquals("empty",
                BeanUtils.methodNameToPropertyName("isEmpty"));
        Assert.assertEquals("length",
                BeanUtils.methodNameToPropertyName("getlength"));
        Assert.assertEquals("length",
                BeanUtils.methodNameToPropertyName("setlength"));
    }

    @Test
    public void testIsArrayType() {
        Assert.assertFalse(BeanUtils.isArrayType(String.class));
    }

    @Test
    public void testIsCollectionType() {
        Assert.assertFalse(BeanUtils.isCollectionType(String.class));
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
                BeanUtils.getCollectionType(parameterizedType));

        Assert.assertNull(BeanUtils.getCollectionType(null));
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

        Assert.assertEquals(String.class,
                BeanUtils.getCollectionType(parameterizedType));
    }

    @Test
    public void testReadObjectProperty()
            throws InvocationTargetException, IllegalAccessException {
        Assert.assertNull(BeanUtils.readObjectProperty(null, ".length"));
        Assert.assertNull(BeanUtils.readObjectProperty(String.class, "bar"));
        Assert.assertNull(BeanUtils.readObjectProperty(String.class, ".length"));

        Assert.assertEquals(String.class,
                BeanUtils.readObjectProperty(String.class, "."));
    }

    @Test
    public void testIsBooleanType() {
        Assert.assertTrue(BeanUtils.isBooleanType(Boolean.TYPE));
        Assert.assertTrue(BeanUtils.isBooleanType(Boolean.class));

        Assert.assertFalse(BeanUtils.isBooleanType(String.class));
        Assert.assertFalse(BeanUtils.isBooleanType(null));
    }

    @Test
    public void testGetDefaultPrimitiveValue() {
        Assert.assertEquals(0L,
                BeanUtils.getDefaultPrimitiveValue(Long.TYPE));
        Assert.assertEquals(0,
                BeanUtils.getDefaultPrimitiveValue(Integer.TYPE));
        Assert.assertEquals(0.0f,
                BeanUtils.getDefaultPrimitiveValue(Float.TYPE));
        Assert.assertEquals(0.0,
                BeanUtils.getDefaultPrimitiveValue(Double.TYPE));
        Assert.assertEquals((short) 0,
                BeanUtils.getDefaultPrimitiveValue(Short.TYPE));
        Assert.assertEquals((byte) 0,
                BeanUtils.getDefaultPrimitiveValue(Byte.TYPE));
        Assert.assertEquals((char) 0,
                BeanUtils.getDefaultPrimitiveValue(Character.TYPE));
        Assert.assertEquals(false,
                BeanUtils.getDefaultPrimitiveValue(Boolean.TYPE));

        thrown.expect(IllegalArgumentException.class);
        BeanUtils.getDefaultPrimitiveValue(String.class);
    }

    @Test
    public void testIsNumericType() {
        Assert.assertTrue(BeanUtils.isNumericType(Long.TYPE));
        Assert.assertTrue(BeanUtils.isNumericType(Byte.TYPE));
        Assert.assertTrue(BeanUtils.isNumericType(Short.TYPE));
        Assert.assertTrue(BeanUtils.isNumericType(Float.TYPE));
        Assert.assertTrue(BeanUtils.isNumericType(Double.TYPE));
        Assert.assertTrue(BeanUtils.isNumericType(Short.class));
        Assert.assertTrue(BeanUtils.isNumericType(Integer.TYPE));

        Assert.assertFalse(BeanUtils.isNumericType(String.class));
    }

    @Test
    public void testInvokeObjectMethod() throws Throwable {
        Assert.assertEquals("0", BeanUtils.invokeObjectMethod("String",
                "valueOf", new Class<?>[]{int.class}, new Object[]{0}));
        Assert.assertEquals(6,
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

        Assert.assertEquals(123, BeanUtils.invokeObjectDeclaredMethod(
            child,
            "getValueA",
            new Class[0],
            new Object[0]
        ));

        Assert.assertEquals(456, BeanUtils.invokeObjectDeclaredMethod(
            child,
            "getValueB",
            new Class[0],
            new Object[0]
        ));

        Assert.assertEquals(0, BeanUtils.invokeObjectDeclaredMethod(
            child,
            "getValueC",
            new Class[0],
            new Object[0]
        ));

        Assert.assertThrows(NoSuchMethodException.class, () -> BeanUtils.invokeObjectDeclaredMethod(
            child,
            "getValueD",
            new Class[0],
            new Object[0]
        ));
    }

    @Test
    public void testInvokeStaticMethod() throws Throwable {
        Assert.assertEquals("0", BeanUtils.invokeStaticMethod(String.class,
                "valueOf", new Class<?>[]{int.class}, new Object[]{0}));
    }
}
