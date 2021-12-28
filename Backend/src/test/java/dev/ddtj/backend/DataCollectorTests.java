package dev.ddtj.backend;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import dev.ddtj.backend.data.ExecutionState;
import dev.ddtj.backend.data.Invocation;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.data.objectmodel.BaseType;
import dev.ddtj.backend.data.objectmodel.BuiltinTypes;
import dev.ddtj.backend.javadebugger.DataCollector;
import dev.ddtj.backend.javadebugger.MonitoredSession;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataCollectorTests {
    @Mock
    private VMDeathEvent vmDeathEvent;

    @Mock
    private MethodEntryEvent methodEntryEvent;

    @Mock
    private MethodExitEvent methodExitEvent;

    @Mock
    private MonitoredSession monitoredSession;

    @Mock
    private Method method;

    @Mock
    private ParentMethod parentMethod;

    @Mock
    private ReferenceType declaringType;

    @Mock
    private ThreadReference threadReference;

    @Mock
    private StackFrame stackFrame;

    @Mock
    private Location location;

    @Mock
    private StringReference stringValue;

    @Test
    void testProcessEvent() throws IncompatibleThreadStateException {
        DataCollector dataCollector = new DataCollector();
        dataCollector.processEvent(monitoredSession, vmDeathEvent, 1);

        Mockito.when(methodEntryEvent.method()).thenReturn(method);
        Mockito.when(methodExitEvent.method()).thenReturn(method);
        Mockito.when(method.declaringType()).thenReturn(declaringType);
        Mockito.when(declaringType.isPublic()).thenReturn(true);
        Mockito.when(method.isConstructor()).thenReturn(false);
        Mockito.when(method.isStaticInitializer()).thenReturn(false);
        Mockito.when(method.isNative()).thenReturn(false);
        Mockito.when(method.isPrivate()).thenReturn(false);
        Mockito.when(method.isProtected()).thenReturn(false);
        Mockito.when(method.isPackagePrivate()).thenReturn(false);
        Mockito.when(parentMethod.getReturnValue()).thenReturn(BuiltinTypes.STRING);
        Mockito.when(declaringType.name()).thenReturn("com.company.ClassName");
        Mockito.when(monitoredSession.getOrCreateMethod(method)).thenReturn(parentMethod);
        ExecutionState executionState = new ExecutionState();
        executionState.setParentMethod(parentMethod);
        executionState.setInvocation(new Invocation());

        Mockito.when(monitoredSession.removeExecutionState(methodExitEvent)).thenReturn(executionState);
        Mockito.when(parentMethod.getParameters()).thenReturn(new BaseType[0]);

        Mockito.when(methodEntryEvent.thread()).thenReturn(threadReference);
        Mockito.when(methodExitEvent.returnValue()).thenReturn(stringValue);
        Mockito.when(stringValue.value()).thenReturn("returnValue");
        Mockito.when(threadReference.frameCount()).thenReturn(1);
        Mockito.when(threadReference.frame(0)).thenReturn(stackFrame);
        Mockito.when(stackFrame.location()).thenReturn(location);
        Mockito.when(stackFrame.getArgumentValues()).thenReturn(Collections.emptyList());
        Mockito.when(location.method()).thenReturn(method);

        dataCollector.processEvent(monitoredSession, methodEntryEvent, 1);
        Mockito.verify(monitoredSession, Mockito.times(2))
                .getOrCreateMethod(method);

        dataCollector.processEvent(monitoredSession, methodExitEvent, 1);
        Mockito.verify(monitoredSession, Mockito.times(2))
                .getOrCreateMethod(method);
    }
}
