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

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Method;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.request.MethodExitRequest;
import dev.ddtj.backend.data.ExecutionState;
import dev.ddtj.backend.data.Invocation;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.data.objectmodel.BaseType;
import dev.ddtj.backend.data.objectmodel.PrimitiveAndWrapperType;
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
        long startTime = System.currentTimeMillis();
        try {
            long invocation = 1;
            session.setSessionId(shortUUID());
            EventSet eventSet = session.getVirtualMachine().eventQueue().remove(100);
            boolean vmDeath = false;
            while (true) {
                for (Event event : eventSet) {
                    if (event instanceof VMDeathEvent) {
                        vmDeath = true;
                        continue;
                    }

                    processEvent(session, event, invocation);
                    invocation++;
                }

                if(vmDeath) {
                    eventSet = session.getVirtualMachine().eventQueue().remove(100);
                    if ((eventSet == null || eventSet.isEmpty())) {
                        session.getVirtualMachine().resume();
                        break;
                    }
                } else {
                    session.getVirtualMachine().resume();
                    eventSet = session.getVirtualMachine().eventQueue().remove(100);
                }
            }
            log.fine("VM Loop Exiting after " + (System.currentTimeMillis() - startTime) + "ms");
        } catch (InterruptedException e) {
            log.log(Level.SEVERE,"Interrupted while waiting for event queue", e);

            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }

    public void processEvent(MonitoredSession session, Event event, long invocationCount) {
        try {
            if (event instanceof MethodEntryEvent) {
                processMethodEntry(session, invocationCount, (MethodEntryEvent) event);
            }

            if (event instanceof MethodExitEvent) {
                MethodExitEvent methodExitEvent = (MethodExitEvent) event;
                Method currentMethod = methodExitEvent.method();
                if(isExcludedMethod(session, currentMethod)) {
                    return;
                }

                ExecutionState executionState = session.removeExecutionState(methodExitEvent);
                ParentMethod parentMethod = executionState.getParentMethod();
                session.validateMethod(currentMethod, parentMethod);
                Invocation invocation = executionState.getInvocation();
                if (parentMethod.getReturnValue() != PrimitiveAndWrapperType.VOID) {
                    invocation.setResult(parentMethod.getReturnValue().getValue(methodExitEvent.returnValue()));
                }
                parentMethod.addInvocation(invocation);
            }
        } catch (IncompatibleThreadStateException e) {
            log.log(Level.SEVERE,"Incompatible thread state", e);
        }
    }

    private boolean isExcludedMethod(MonitoredSession session, Method method) {
        return method.isConstructor() || method.isStaticInitializer() || method.isNative() || method.isPrivate() ||
                method.isProtected() || method.isPackagePrivate() || !method.declaringType().isPublic() ||
                session.isExcluded(method.declaringType().name());
    }

    private void processMethodEntry(MonitoredSession session, long invocationCount, MethodEntryEvent methodEntryEvent) throws IncompatibleThreadStateException {
        Method method = methodEntryEvent.method();
        if(isExcludedMethod(session, method)) {
            return;
        }

        ParentMethod parent = session.getOrCreateMethod(methodEntryEvent.method());
        parent.setApplicable(true);
        Invocation invocation = new Invocation();

        ThreadReference threadReference = methodEntryEvent.thread();
        ParentMethod[] stack = new ParentMethod[threadReference.frameCount()];
        for (int i = 0; i < stack.length; i++) {
            stack[i] = session.getOrCreateMethod(threadReference.frame(i).location().method());
        }

        invocation.setStack(stack);
        invocation.setThreadId(threadReference.uniqueID());

        invocation.setTime(System.currentTimeMillis());
        List<Value> valueList = threadReference.frame(0).getArgumentValues();
        Object[] arguments = convertArgumentsToArray(parent, valueList);
        invocation.setArguments(arguments);

        invocation.setId(session.getSessionId() + invocationCount);

        ExecutionState executionState = new ExecutionState();
        executionState.setInvocation(invocation);
        executionState.setParentMethod(parent);
        executionState.setClassName(method.declaringType().name());

        session.queueExecutionState(methodEntryEvent, executionState);
    }

    private Object[] convertArgumentsToArray(ParentMethod parent, List<Value> valueList) {
        BaseType[] parameters = parent.getParameters();
        Object[] arguments = new Object[parameters.length];
        for(int iter = 0 ; iter < parameters.length ; iter++) {
            arguments[iter] = parameters[iter].getValue(valueList.get(iter));
        }
        return arguments;
    }
}
