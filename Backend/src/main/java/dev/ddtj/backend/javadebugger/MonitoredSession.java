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

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import dev.ddtj.backend.data.ExecutionState;
import dev.ddtj.backend.data.ParentClass;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.data.objectmodel.BaseType;
import dev.ddtj.backend.data.objectmodel.TypeFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.java.Log;

@Log
public class MonitoredSession {
    private static final Object LOCK = new Object();
    private final VirtualMachine virtualMachine;
    private final String filter;
    private final Map<String, ParentClass> parentClassMap = new HashMap<>();
    private String sessionId;
    private String[] excludeList = {
            "java.",
            "com.sun.",
            "jdk.",
            "sun."
    };
    private final Map<String, ExecutionState> pendingExecutions = new HashMap<>();

    public MonitoredSession(VirtualMachine virtualMachine, String filter) {
        this.virtualMachine = virtualMachine;
        this.filter = filter;
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public boolean isExcluded(String className) {
        for(String prefix : excludeList) {
            if(className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private ParentMethod createMethod(ParentClass parentClass, String methodSignature, Method method) {
        ParentMethod parentMethod = new ParentMethod();
        parentMethod.setParentClass(parentClass);
        parentMethod.setSignature(methodSignature);
        parentMethod.setName(method.name());
        initMethodImpl(method, parentMethod);
        return parentMethod;
    }

    public void validateMethod(Method method, ParentMethod parentMethod) {
        if(parentMethod.isInitializationFailure()) {
            parentMethod.setInitializationFailure(false);
            initMethodImpl(method, parentMethod);
            if(parentMethod.isInitializationFailure()) {
                throw new IllegalStateException("Failed to initialize method: " + parentMethod.fullName());
            }
        }
    }

    private void initMethodImpl(Method method, ParentMethod parentMethod) {
        try {
            if(method.isNative() || method.isStaticInitializer()) {
                parentMethod.setParameters(new BaseType[0]);
                parentMethod.setReturnValue(TypeFactory.create(method.returnType()));
            } else {
                BaseType[] parameters = TypeFactory.create(method.argumentTypes());
                parentMethod.setParameters(parameters);
                parentMethod.setReturnValue(TypeFactory.create(method.returnType()));
            }
        } catch (ClassNotLoadedException classNotLoadedException) {
            log.info("Type in argument or return value isn't loaded yet: " + method.argumentTypeNames() +
                    " return value: " + method.returnTypeName() + " for method " + method.name() + " of " +
                    method.declaringType().name());
            parentMethod.setInitializationFailure(true);
        }
    }


    public ParentMethod getOrCreateMethod(Method method) {
        String className = method.declaringType().name();
        String methodSignature = method.signature();
        String methodName = method.name();
        ParentClass parentClass = null;
        synchronized (LOCK) {
            parentClass = parentClassMap.get(className);
            if (parentClass == null) {
                parentClass = new ParentClass();
                parentClass.setName(className);
                ParentMethod parentMethod = createMethod(parentClass, methodSignature, method);
                parentClass.addMethod(parentMethod);
                parentClassMap.put(className, parentClass);
                return parentMethod;
            }
        }

        synchronized (parentClass) {
            ParentMethod parentMethod = parentClass.findMethod(methodName + methodSignature);
            if (parentMethod != null) {
                return parentMethod;
            }
            parentMethod = createMethod(parentClass, methodSignature, method);
            parentClass.addMethod(parentMethod);
            return parentMethod;
        }
    }

    public Collection<ParentClass> listClasses() {
        synchronized (LOCK) {
            return parentClassMap.values();
        }
    }

    public ParentClass getClass(String className) {
        synchronized (LOCK) {
            return parentClassMap.get(className);
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        if(this.sessionId != null) {
            throw new IllegalStateException("Session id already set");
        }
        this.sessionId = sessionId;
    }

    public void queueExecutionState(MethodEntryEvent event, ExecutionState executionState) throws IncompatibleThreadStateException {
        ThreadReference thread = event.thread();
        pendingExecutions.put(generateInvocationSignature(thread), executionState);
    }

    public ExecutionState removeExecutionState(MethodExitEvent event) throws IncompatibleThreadStateException {
        ThreadReference thread = event.thread();
        return pendingExecutions.remove(generateInvocationSignature(thread));
    }

    private String generateInvocationSignature(ThreadReference thread) throws IncompatibleThreadStateException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(thread.uniqueID());
        ObjectReference ref = thread.frame(0).thisObject();
        if(ref != null) {
            stringBuilder.append("-");
            stringBuilder.append(ref.uniqueID());
        }
        int frameCount = thread.frameCount();
        stringBuilder.append(frameCount);
        for (int i = 0; i < frameCount; i++) {
            StackFrame frame = thread.frame(i);
            Method method = frame.location().method();
            stringBuilder.append(method.declaringType().name());
            stringBuilder.append(method.name());
            stringBuilder.append(method.signature());
        }
        return stringBuilder.toString();
    }
}
