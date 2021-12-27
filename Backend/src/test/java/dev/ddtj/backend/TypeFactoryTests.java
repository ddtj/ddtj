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

import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import dev.ddtj.backend.data.objectmodel.BaseType;
import dev.ddtj.backend.data.objectmodel.PrimitiveAndWrapperType;
import dev.ddtj.backend.data.objectmodel.TypeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(MockitoExtension.class)
class TypeFactoryTests {

    @Test
    void testTypeFactory() {
        testPrimitive("boolean");
        testPrimitive("byte");
        testPrimitive("char");
        testPrimitive("short");
        testPrimitive("int");
        testPrimitive("long");
        testPrimitive("float");
        testPrimitive("double");

        testPrimitiveWrapper(Boolean.class.getName());
        testPrimitiveWrapper(Byte.class.getName());
        testPrimitiveWrapper(Character.class.getName());
        testPrimitiveWrapper(Short.class.getName());
        testPrimitiveWrapper(Integer.class.getName());
        testPrimitiveWrapper(Long.class.getName());
        testPrimitiveWrapper(Float.class.getName());
        testPrimitiveWrapper(Double.class.getName());
    }

    private void testPrimitive(String name) {
        PrimitiveType primitiveType = Mockito.mock(PrimitiveType.class);
        Mockito.when(primitiveType.name()).thenReturn(name);
        BaseType baseType = TypeFactory.create(primitiveType);
        assertEquals(name, baseType.getType());
        assertInstanceOf(PrimitiveAndWrapperType.class, baseType);
        assertFalse(((PrimitiveAndWrapperType)baseType).isWrapper());
    }

    private void testPrimitiveWrapper(String name) {
        ReferenceType type = Mockito.mock(ReferenceType.class);
        Mockito.when(type.name()).thenReturn(name);
        BaseType baseType = TypeFactory.create(type);
        assertEquals(name, baseType.getType());
        assertInstanceOf(PrimitiveAndWrapperType.class, baseType);
        assertTrue(((PrimitiveAndWrapperType)baseType).isWrapper());
    }

}
