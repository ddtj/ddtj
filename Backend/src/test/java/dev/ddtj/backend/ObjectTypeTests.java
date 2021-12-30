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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.IntegerType;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import dev.ddtj.backend.data.objectmodel.ObjectType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectTypeTests {
    private static final String CLASS_NAME = "pkg.ClassName";

    @Mock
    private IntegerType integerType;

    @Test
    void inspectObjectTest() throws ClassNotLoadedException, AbsentInformationException {
        ReferenceType type = Mockito.mock(ReferenceType.class);
        Mockito.when(type.isAbstract()).thenReturn(true);
        assertEquals(ObjectType.CreationType.ABSTRACT, ObjectType.create(type).getCreationType());

        type = Mockito.mock(ReferenceType.class);
        Mockito.when(type.isPrivate()).thenReturn(true);
        assertEquals(ObjectType.CreationType.PRIVATE, ObjectType.create(type).getCreationType());

        type = Mockito.mock(ReferenceType.class);
        Mockito.when(type.isPackagePrivate()).thenReturn(true);
        assertEquals(ObjectType.CreationType.PACKAGE_PRIVATE, ObjectType.create(type).getCreationType());


        Mockito.when(integerType.name()).thenReturn("int");
        Field field1 = create("field1", integerType);
        Field field2 = create("field2", integerType);
        List<Field> fields = Arrays.asList(field1, field2);

        List<Method> methods = Arrays.asList(create(integerType, "setField1", "void", "int"),
                create(integerType, "setField2", "int", "int"),
                create(integerType, "<init>", "void"));

        type = createClass(methods, fields);
        ObjectType settersType = ObjectType.create(type);
        assertEquals(ObjectType.CreationType.SETTERS, settersType.getCreationType());
        assertTrue(settersType.canObjectBeCreated());

        ObjectReference object = Mockito.mock(ObjectReference.class);
        ReferenceType objectType = Mockito.mock(ReferenceType.class);
        IntegerValue value = Mockito.mock(IntegerValue.class);
        Mockito.when(object.referenceType()).thenReturn(objectType);
        Mockito.when(object.getValue(field1)).thenReturn(value);
        Mockito.when(object.getValue(field2)).thenReturn(value);
        Mockito.when(objectType.fieldByName("field1")).thenReturn(field1);
        Mockito.when(objectType.fieldByName("field2")).thenReturn(field2);
        Mockito.when(value.value()).thenReturn(1);
        settersType.getValue(object);

        assertEquals(2, settersType.getFieldValues(object).length);

        assertEquals("x", settersType.getCodeRepresentation("x", null));
        assertEquals("field1", settersType.getFieldName(0));

        assertEquals(5, Array.getLength(settersType.allocateArray(5)));

        type = createClass(methods.subList(0, 1), fields);
        assertEquals(ObjectType.CreationType.NO_VALID_CONSTRUCTOR, ObjectType.create(type).getCreationType());

        methods = Arrays.asList(
                create(integerType, "<init>", "void", "field1", "field2"));
        type = createClass(methods, fields);
        assertEquals(ObjectType.CreationType.CONSTRUCTOR_FACTORY, ObjectType.create(type).getCreationType());
    }

    public static Field create(String name, Type type) throws ClassNotLoadedException {
        Field field = Mockito.mock(Field.class);
        Mockito.when(field.name()).thenReturn(name);
        Mockito.when(field.typeName()).thenReturn("int");
        Mockito.when(field.type()).thenReturn(type);
        return field;
    }

    private static List<Type> createIntList(IntegerType integerType, int count) {
        List<Type> types = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            types.add(integerType);
        }
        return types;
    }

    private static List<String> createList(int count, String text) {
        List<String> types = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            types.add(text);
        }
        return types;
    }

    private static List<LocalVariable> createLocalVariables(String... args) {
        List<LocalVariable> types = new ArrayList<>();
        for (String arg : args) {
            LocalVariable localVariable = new LocalVariable() {
                @Override
                public String name() {
                    return arg;
                }

                @Override
                public String typeName() {
                    return "int";
                }

                @Override
                public Type type() throws ClassNotLoadedException {
                    return null;
                }

                @Override
                public String signature() {
                    return null;
                }

                @Override
                public String genericSignature() {
                    return null;
                }

                @Override
                public boolean isVisible(StackFrame frame) {
                    return false;
                }

                @Override
                public boolean isArgument() {
                    return false;
                }

                @Override
                public VirtualMachine virtualMachine() {
                    return null;
                }

                @Override
                public int compareTo(LocalVariable o) {
                    return 0;
                }
            };
            types.add(localVariable);
        }
        return types;
    }

    public static Method create(IntegerType integerType, String name, String returnType, String... args) throws ClassNotLoadedException, AbsentInformationException {
        Method method = Mockito.mock(Method.class);
        Mockito.when(method.name()).thenReturn(name);
        Mockito.lenient().when(method.returnTypeName()).thenReturn(returnType);
        Mockito.lenient().when(method.argumentTypes()).thenReturn(createIntList(integerType, args.length));
        Mockito.when(method.argumentTypeNames()).thenReturn(createList(args.length, "int"));
        Mockito.lenient().when(method.arguments()).thenReturn(createLocalVariables(args));
        Mockito.when(method.isPublic()).thenReturn(true);
        return method;
    }

    public static ReferenceType createClass(List<Method> methodList, List<Field> fields) {
        ReferenceType type = Mockito.mock(ReferenceType.class);
        Mockito.when(type.name()).thenReturn(CLASS_NAME);
        Mockito.when(type.methods()).thenReturn(methodList);
        Mockito.when(type.allFields()).thenReturn(fields);
        return type;
    }
}
