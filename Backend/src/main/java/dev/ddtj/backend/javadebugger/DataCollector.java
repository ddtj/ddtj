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
package dev.ddtj.backend.javadebugger;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.StepRequest;
import dev.ddtj.backend.data.ExecutionState;
import dev.ddtj.backend.data.Invocation;
import dev.ddtj.backend.data.MockInvocation;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.data.objectmodel.BaseType;
import dev.ddtj.backend.data.objectmodel.PrimitiveAndWrapperType;
import dev.ddtj.backend.data.objectmodel.TypeFactory;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Log
public class DataCollector {
    private static final String EXECUTION_STATE = "EXECUTION_STATE";

    private String shortUUID() {
        // Based on code from https://stackoverflow.com/a/21103563/756809
        UUID uuid = UUID.randomUUID();
        ByteBuffer uuidBuffer = ByteBuffer.allocate(16);
        LongBuffer longBuffer = uuidBuffer.asLongBuffer();
        longBuffer.put(uuid.getMostSignificantBits());
        longBuffer.put(uuid.getLeastSignificantBits());
        String encoded = new String(Base64.getEncoder().encode(uuidBuffer.array()),
                StandardCharsets.US_ASCII);
        encoded = encoded.replace('+', '.')
                .replace('/', '_')
                .replace('=', '-');
        return encoded;
    }

    @Async
    public void collect(MonitoredSession session) {
        try {
            long invocation = 1;
            session.setSessionId(shortUUID());
            EventSet eventSet = session.getVirtualMachine().eventQueue().remove(100);
            while (eventSet != null) {
                for (Event event : eventSet) {
                    log.fine("Debug event " + event);
                    if (processEvent(session, event, invocation)) {
                        return;
                    }
                    invocation++;
                }
                session.getVirtualMachine().resume();
                eventSet = session.getVirtualMachine().eventQueue().remove(100);
            }
            log.fine("VM Loop Exiting");
        } catch (InterruptedException e) {
            log.log(Level.SEVERE,"Interrupted while waiting for event queue", e);

            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }

    public boolean processEvent(MonitoredSession session, Event event, long invocationCount) {
        try {
            if (event instanceof VMDeathEvent) {
                return true;
            }
            if (event instanceof MethodEntryEvent) {
                MethodEntryEvent methodEntryEvent = (MethodEntryEvent) event;
                Method currentMethod = methodEntryEvent.method();

                processMethodEntry(session, event, invocationCount, methodEntryEvent, currentMethod);
            }

            if (event instanceof MethodExitEvent) {
                MethodExitEvent methodExitEvent = (MethodExitEvent) event;

                ExecutionState executionState = (ExecutionState)methodExitEvent.request().getProperty(EXECUTION_STATE);
                ParentMethod parentMethod = executionState.getParentMethod();
                Invocation invocation = executionState.getInvocation();
                if (parentMethod.getReturnValue() != PrimitiveAndWrapperType.VOID) {
                    invocation.setResult(parentMethod.getReturnValue().getValue(methodExitEvent.returnValue()));
                }
                parentMethod.addInvocation(invocation);

                StepRequest stepRequest = executionState.getStepRequest();
                stepRequest.disable();
                event.virtualMachine().eventRequestManager().deleteEventRequest(stepRequest);
            }

            if (event instanceof StepEvent) {
                StepEvent stepEvent = (StepEvent) event;
                ExecutionState executionState = (ExecutionState)stepEvent.request().getProperty(EXECUTION_STATE);
                Method method = stepEvent.thread().frame(0).location().method();
                if(!method.declaringType().name().equals(executionState.getClassName())) {
                    // we stepped into a new method. We need to step out and get a result
                    StackFrame frame = stepEvent.thread().frame(0);

                    MockInvocation mockInvocation = new MockInvocation();
                    ParentMethod parentMethod = session.getOrCreateMethod(method);
                    mockInvocation.setParentMethod(parentMethod);
                    mockInvocation.setParentClass(session.getClass(method.declaringType().name()));
                    List<Value> argumentsToInvocation = frame.getArgumentValues();
                    mockInvocation.setArguments(convertArgumentsToArray(parentMethod, argumentsToInvocation));

                    executionState.setCurrentMockInvocation(mockInvocation);

                    event.virtualMachine().eventRequestManager().deleteEventRequest(stepEvent.request());
                    createStepRequest(executionState, event, stepEvent.thread(), StepRequest.STEP_OUT);
                    return false;
                }

                if(executionState.getCurrentMockInvocation() != null) {
                    // we need to get the result of the previous invocation
                    try {
                        log.fine("" + stepEvent.thread().frame(0).visibleVariables());
                    } catch (AbsentInformationException e) {
                        log.fine("No visible variables");
                    }
                }
            }
        } catch (IncompatibleThreadStateException e) {
            log.log(Level.SEVERE,"Incompatible thread state", e);
        }

        return false;
    }

    private void processMethodEntry(MonitoredSession session, Event event, long invocationCount, MethodEntryEvent methodEntryEvent, Method currentMethod) throws IncompatibleThreadStateException {
        Method method = methodEntryEvent.method();
        if(method.isConstructor() || method.isStaticInitializer() || method.isNative() || method.isPrivate() ||
                method.isProtected() || !method.declaringType().isPublic()) {
            return;
        }

        ParentMethod parent = session.getOrCreateMethod(methodEntryEvent.method());
        parent.setApplicable(true);
        Invocation invocation = new Invocation();
        invocation.setTime(System.currentTimeMillis());
        List<Value> valueList = methodEntryEvent.thread().frame(0).getArgumentValues();
        Object[] arguments = convertArgumentsToArray(parent, valueList);
        invocation.setArguments(arguments);

        invocation.setId(session.getSessionId() + invocationCount);

        ExecutionState executionState = new ExecutionState();
        executionState.setInvocation(invocation);
        executionState.setParentMethod(parent);
        executionState.setClassName(method.declaringType().name());

        createStepRequest(executionState, event, methodEntryEvent.thread(), StepRequest.STEP_INTO);

        // TODO: Maybe this can be non-blocking?
        MethodExitRequest methodExitRequest = event.virtualMachine().eventRequestManager().createMethodExitRequest();
        limitBreakpointScope(methodEntryEvent, currentMethod, methodExitRequest);
        methodExitRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        methodExitRequest.addThreadFilter(methodEntryEvent.thread());
        methodExitRequest.putProperty(EXECUTION_STATE, executionState);
        methodExitRequest.enable();
    }

    private Object[] convertArgumentsToArray(ParentMethod parent, List<Value> valueList) {
        BaseType[] parameters = parent.getParameters();
        Object[] arguments = new Object[parameters.length];
        for(int iter = 0 ; iter < parameters.length ; iter++) {
            arguments[iter] = parameters[iter].getValue(valueList.get(iter));
        }
        return arguments;
    }

    private void createStepRequest(ExecutionState executionState, Event event, ThreadReference threadReference,
                                          int stepType) {
        StepRequest stepRequest = event.virtualMachine().eventRequestManager().createStepRequest(
                threadReference, StepRequest.STEP_LINE, stepType);
        stepRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        stepRequest.putProperty(EXECUTION_STATE, executionState);
        executionState.setStepRequest(stepRequest);
        stepRequest.enable();
    }

    private void limitBreakpointScope(MethodEntryEvent methodEntryEvent, Method currentMethod, MethodExitRequest methodExitRequest) throws IncompatibleThreadStateException {
        if(currentMethod.isStatic() || currentMethod.isStaticInitializer()) {
            methodExitRequest.addClassFilter(currentMethod.declaringType().name());
        } else {
            methodExitRequest.addInstanceFilter(methodEntryEvent.thread().frame(0).thisObject());
        }
    }

}
