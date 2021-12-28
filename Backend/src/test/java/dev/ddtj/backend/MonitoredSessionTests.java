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
import com.sun.jdi.IntegerType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.javadebugger.MonitoredSession;
import java.util.ArrayList;
import lombok.extern.java.Log;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Log
class MonitoredSessionTests {
    @Mock
    private VirtualMachine virtualMachine;

    @Mock
    private Method method;

    @Mock
    private LocalVariable localVariable;

    @Mock
    private ReferenceType referenceType;

    @Mock
    private IntegerType integerType;

    private static final String DECLARING_CLASS = "test.MyTestClass";
    private static final String METHOD_SIGNATURE = "testMethodName()";

    @Test
    void enteringMethodTest() throws AbsentInformationException, ClassNotLoadedException {
        MonitoredSession session = initSession();
        assertSame(session.getOrCreateMethod(method), session.getOrCreateMethod(method));
    }

    @Test()
    void absentInformationExceptionTest() throws AbsentInformationException, ClassNotLoadedException {
        MonitoredSession session = initSession();
        Mockito.lenient().when(method.arguments()).thenThrow(new AbsentInformationException());
        log.severe("There should be a stack trace for AbsentInformationException, this is valid and part of the test");
        assertSame(session.getOrCreateMethod(method), session.getOrCreateMethod(method));
    }

    @Test
    void validateMethodTest() throws ClassNotLoadedException, AbsentInformationException {
        MonitoredSession session = initSession();
        Mockito.lenient().when(method.arguments()).thenReturn(new ArrayList<>());
        ParentMethod parentMethod = session.getOrCreateMethod(method);
        parentMethod.setInitializationFailure(true);
        session.validateMethod(method, parentMethod);
        assertEquals(0, parentMethod.getParameters().length);
    }

    @Test
    void validateMethodIsNativeTest() throws ClassNotLoadedException, AbsentInformationException {
        MonitoredSession session = initSession();
        Mockito.lenient().when(method.isNative()).thenReturn(true);
        ParentMethod parentMethod = session.getOrCreateMethod(method);
        assertEquals(0, parentMethod.getParameters().length);
    }

    @Test
    void validateMethodIsStaticInitializerTest() throws ClassNotLoadedException, AbsentInformationException {
        MonitoredSession session = initSession();
        Mockito.lenient().when(method.isStaticInitializer()).thenReturn(true);
        ParentMethod parentMethod = session.getOrCreateMethod(method);
        assertEquals(0, parentMethod.getParameters().length);
    }

    @Test
    void validateMethodExceptionTest() throws ClassNotLoadedException, AbsentInformationException {
        MonitoredSession session = initSession();
        Mockito.lenient().when(method.arguments()).thenReturn(new ArrayList<>());
        Mockito.lenient().when(method.returnType()).thenThrow(ClassNotLoadedException.class);
        ParentMethod parentMethod = session.getOrCreateMethod(method);
        assertTrue(parentMethod.isInitializationFailure());
    }

    @Test
    void listClassTest() {
        MonitoredSession session = new MonitoredSession(virtualMachine, "test.*");
        assertEquals(0, session.listClasses().size());
    }

    @Test
    void listMethodTest() {
        MonitoredSession session = new MonitoredSession(virtualMachine, "test.*");
        assertNull(session.getClass("DummyClass"));
    }

    @Test
    void sessionIdTest() {
        MonitoredSession session = new MonitoredSession(virtualMachine, "test.*");
        assertNull(session.getSessionId());
        session.setSessionId("X");
        assertEquals("X", session.getSessionId());
        try {
            session.setSessionId("Y");
        } catch (IllegalStateException e) {
            return;
        }
        fail("IllegalStateException should have been thrown");
    }

    private MonitoredSession initSession() throws ClassNotLoadedException {
        MonitoredSession session = new MonitoredSession(virtualMachine, "test.*");
        Mockito.when(referenceType.name()).thenReturn(DECLARING_CLASS);
        Mockito.when(method.declaringType()).thenReturn(referenceType);
        Mockito.when(method.signature()).thenReturn(METHOD_SIGNATURE);
        Mockito.when(method.returnType()).thenReturn(integerType);
        Mockito.lenient().when(integerType.name()).thenReturn("int");
        return session;
    }
}
