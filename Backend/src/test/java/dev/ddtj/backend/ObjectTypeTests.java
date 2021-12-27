package dev.ddtj.backend;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.IntegerType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import static org.junit.jupiter.api.Assertions.*;

import com.sun.jdi.StackFrame;
import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;
import dev.ddtj.backend.data.objectmodel.ObjectType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        List<Field> fields = Arrays.asList(create("field1", integerType), create("field2", integerType));

        List<Method> methods = Arrays.asList(create("setField1", "void", "int"),
                create("setField2", "int", "int"),
                create("<init>", "void"));

        type = createClass(methods, fields);
        assertEquals(ObjectType.CreationType.SETTERS, ObjectType.create(type).getCreationType());

        type = createClass(methods.subList(0, 1), fields);
        assertEquals(ObjectType.CreationType.NO_VALID_CONSTRUCTOR, ObjectType.create(type).getCreationType());

        methods = Arrays.asList(
                create("<init>", "void", "field1", "field2"));
        type = createClass(methods, fields);
        assertEquals(ObjectType.CreationType.CONSTRUCTOR_FACTORY, ObjectType.create(type).getCreationType());
    }

    private Field create(String name, Type type) throws ClassNotLoadedException {
        Field field = Mockito.mock(Field.class);
        Mockito.when(field.name()).thenReturn(name);
        Mockito.when(field.typeName()).thenReturn("int");
        Mockito.when(field.type()).thenReturn(type);
        return field;
    }

    private List<Type> createIntList(int count) {
        List<Type> types = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            types.add(integerType);
        }
        return types;
    }

    private List<String> createList(int count, String text) {
        List<String> types = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            types.add(text);
        }
        return types;
    }

    private List<LocalVariable> createLocalVariables(String... args) {
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

    private Method create(String name, String returnType, String... args) throws ClassNotLoadedException, AbsentInformationException {
        Method method = Mockito.mock(Method.class);
        Mockito.when(method.name()).thenReturn(name);
        Mockito.lenient().when(method.returnTypeName()).thenReturn(returnType);
        Mockito.lenient().when(method.argumentTypes()).thenReturn(createIntList(args.length));
        Mockito.when(method.argumentTypeNames()).thenReturn(createList(args.length, "int"));
        Mockito.lenient().when(method.arguments()).thenReturn(createLocalVariables(args));
        Mockito.when(method.isPublic()).thenReturn(true);
        return method;
    }

    private ReferenceType createClass(List<Method> methodList, List<Field> fields) {
        ReferenceType type = Mockito.mock(ReferenceType.class);
        Mockito.when(type.name()).thenReturn(CLASS_NAME);
        Mockito.when(type.methods()).thenReturn(methodList);
        Mockito.when(type.allFields()).thenReturn(fields);
        return type;
    }
}
