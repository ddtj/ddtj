/**
 * MIT License Copyright (c) 2021, Shai Almog
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package dev.ddtj.backend;

import com.sun.jdi.ArrayType;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.Value;
import com.sun.jdi.VoidType;
import dev.ddtj.backend.data.objectmodel.ArrayObjectOrPrimitiveType;
import dev.ddtj.backend.data.objectmodel.BaseType;
import dev.ddtj.backend.data.objectmodel.BuiltinTypes;
import dev.ddtj.backend.data.objectmodel.PrimitiveAndWrapperType;
import dev.ddtj.backend.data.objectmodel.TypeFactory;
import java.lang.reflect.Array;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(MockitoExtension.class)
class TypeFactoryTests {

    @Test
    void testTypeFactory() throws ClassNotLoadedException {
        BooleanValue booleanValue = Mockito.mock(BooleanValue.class);
        Mockito.when(booleanValue.value()).thenReturn(true);
        testPrimitive("boolean", booleanValue);

        ByteValue byteValue = Mockito.mock(ByteValue.class);
        Mockito.when(byteValue.value()).thenReturn((byte)1);
        testPrimitive("byte", byteValue);

        CharValue charValue = Mockito.mock(CharValue.class);
        Mockito.when(charValue.value()).thenReturn('a');
        testPrimitive("char", charValue);

        ShortValue shortValue = Mockito.mock(ShortValue.class);
        Mockito.when(shortValue.value()).thenReturn((short)1);
        testPrimitive("short", shortValue);

        IntegerValue integerValue = Mockito.mock(IntegerValue.class);
        Mockito.when(integerValue.value()).thenReturn(1);
        testPrimitive("int", integerValue);

        LongValue longValue = Mockito.mock(LongValue.class);
        Mockito.when(longValue.value()).thenReturn(1L);
        testPrimitive("long", longValue);

        FloatValue floatValue = Mockito.mock(FloatValue.class);
        Mockito.when(floatValue.value()).thenReturn(1.0f);
        testPrimitive("float", floatValue);

        DoubleValue doubleValue = Mockito.mock(DoubleValue.class);
        Mockito.when(doubleValue.value()).thenReturn(1.0);
        testPrimitive("double", doubleValue);

        testPrimitiveWrapper(Boolean.class.getName());
        testPrimitiveWrapper(Byte.class.getName());
        testPrimitiveWrapper(Character.class.getName());
        testPrimitiveWrapper(Short.class.getName());
        testPrimitiveWrapper(Integer.class.getName());
        testPrimitiveWrapper(Long.class.getName());
        testPrimitiveWrapper(Float.class.getName());
        testPrimitiveWrapper(Double.class.getName());

        VoidType voidType = Mockito.mock(VoidType.class);
        BaseType voidBaseType = TypeFactory.create(voidType);
        assertEquals(PrimitiveAndWrapperType.VOID, voidBaseType);

        ReferenceType referenceType = Mockito.mock(ReferenceType.class);
        Mockito.when(referenceType.name()).thenReturn("java.lang.String");
        assertEquals(BuiltinTypes.STRING, TypeFactory.create(referenceType));

        ArrayType arrayType = Mockito.mock(ArrayType.class);
        PrimitiveType intType = Mockito.mock(PrimitiveType.class);
        Mockito.when(intType.name()).thenReturn("int");
        Mockito.when(arrayType.componentType()).thenReturn(intType);
        Mockito.when(arrayType.name()).thenReturn("I[");
        assertInstanceOf(ArrayObjectOrPrimitiveType.class, TypeFactory.create(arrayType));
    }

    private void testPrimitive(String name, Value primitiveValue) {
        PrimitiveType primitiveType = Mockito.mock(PrimitiveType.class);
        Mockito.when(primitiveType.name()).thenReturn(name);
        BaseType baseType = TypeFactory.create(primitiveType);
        assertEquals(name, baseType.getType());
        assertInstanceOf(PrimitiveAndWrapperType.class, baseType);
        assertFalse(((PrimitiveAndWrapperType)baseType).isWrapper());
        Object primitiveArray = baseType.allocateArray(5);
        baseType.setArrayValue(primitiveArray, 0, primitiveValue);
        Object value = baseType.getValue(primitiveValue);
        assertEquals(5, Array.getLength(primitiveArray));
        assertNotNull(baseType.getCodeRepresentation("test", value));
    }

    private void testPrimitiveWrapper(String name) {
        ReferenceType type = Mockito.mock(ReferenceType.class);
        Mockito.when(type.name()).thenReturn(name);
        BaseType baseType = TypeFactory.create(type);
        assertEquals(name, baseType.getType());
        assertInstanceOf(PrimitiveAndWrapperType.class, baseType);
        assertTrue(((PrimitiveAndWrapperType)baseType).isWrapper());
        assertEquals(5, Array.getLength(baseType.allocateArray(5)));
    }
}
