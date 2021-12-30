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

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.IntegerType;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import dev.ddtj.backend.data.Invocation;
import dev.ddtj.backend.data.ParentClass;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.data.objectmodel.BaseType;
import dev.ddtj.backend.data.objectmodel.ObjectType;
import dev.ddtj.backend.data.objectmodel.PrimitiveAndWrapperType;
import dev.ddtj.backend.service.TestGenerator;
import dev.ddtj.backend.testdata.BasicApp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@Log
class TestGeneratorTests {
    private static final String CLASS_NAME = "pkg.ClassName";

    @Mock
    private IntegerType integerType;

    @Test
    void testGeneratorTest() throws ClassNotLoadedException, AbsentInformationException {
        Mockito.when(integerType.name()).thenReturn("int");
        List<Field> fields = Arrays.asList(ObjectTypeTests.create("field1", integerType),
                ObjectTypeTests.create("field2", integerType));


        List<Method> methods = Arrays.asList(ObjectTypeTests.create(integerType, "setField1", "void", "int"),
                ObjectTypeTests.create(integerType, "setField2", "int", "int"),
                ObjectTypeTests.create(integerType, "<init>", "void"));

        ReferenceType referenceType = ObjectTypeTests.createClass(methods, fields);
        ObjectType settersType = ObjectType.create(referenceType);

        ParentClass parentClass = new ParentClass();
        parentClass.setName(BasicApp.class.getName());
        parentClass.setObjectType(settersType);
        ParentMethod parentMethod = new ParentMethod();
        parentClass.addMethod(parentMethod);
        parentMethod.setParentClass(parentClass);
        parentMethod.setParameters(new BaseType[] { PrimitiveAndWrapperType.INTEGER, PrimitiveAndWrapperType.INTEGER });
        parentMethod.setName("test");
        parentMethod.setSignature("V()");
        parentMethod.setReturnType(PrimitiveAndWrapperType.VOID);
        parentMethod.setApplicable(true);

        Invocation invocation = new Invocation();
        parentMethod.addInvocation(invocation);
        invocation.setFields(new Object[0]);
        invocation.setTime(System.currentTimeMillis() - 500);
        invocation.setEndTime(System.currentTimeMillis());
        invocation.setThreadId(1);
        invocation.setArguments(new Object[] { 1, 2 });

        List<Invocation> internalCalls = new ArrayList<>();
        TestGenerator testGenerator = new TestGenerator(parentClass, parentMethod, invocation, internalCalls);
        Set<String> imports = testGenerator.getCustomImports();
        List<String> mocks = testGenerator.getMocks();
        List<String> argumentInitialization = testGenerator.getArgumentInitialization();
        String arguments = testGenerator.getArguments();

        List<String> creationCode = testGenerator.getCreationCode();
        assertEquals(1, creationCode.size());
        assertEquals("ClassName myObjectInstance = new ClassName();", creationCode.get(0));

        String methodName = testGenerator.getMethodName();
        assertEquals("test", methodName);
    }
}
