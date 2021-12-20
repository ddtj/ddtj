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

import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import dev.ddtj.backend.javadebugger.MonitoredSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonitoredSessionTests {
    @Mock
    private VirtualMachine virtualMachine;

    @Mock
    private Method method;

    @Mock
    private ReferenceType referenceType;

    private static final String DECLARING_CLASS = "test.MyTestClass";
    private static final String METHOD_SIGNATURE = "testMethodName()";

    @Test
    void enteringMethodTest() {
        MonitoredSession session = new MonitoredSession(virtualMachine, "test.*");
        Mockito.when(referenceType.name()).thenReturn(DECLARING_CLASS);
        Mockito.when(method.declaringType()).thenReturn(referenceType);
        Mockito.when(method.signature()).thenReturn(METHOD_SIGNATURE);

        Assertions.assertSame(session.enteringMethod(method), session.enteringMethod(method));
    }
}
