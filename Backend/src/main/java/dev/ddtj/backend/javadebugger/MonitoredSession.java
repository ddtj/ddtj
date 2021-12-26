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
import com.sun.jdi.Method;
import com.sun.jdi.VirtualMachine;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.data.objectmodel.BaseType;
import dev.ddtj.backend.data.objectmodel.TypeFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import dev.ddtj.backend.data.ParentClass;
import java.util.logging.Level;
import lombok.extern.java.Log;

@Log
public class MonitoredSession {
    private static final Object LOCK = new Object();
    private final VirtualMachine virtualMachine;
    private final String filter;
    private final Map<String, ParentClass> parentClassMap = new HashMap<>();
    private String sessionId;

    public MonitoredSession(VirtualMachine virtualMachine, String filter) {
        this.virtualMachine = virtualMachine;
        this.filter = filter;
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public String getFilter() {
        return filter;
    }


    private ParentMethod createMethod(String methodSignature, Method method) {
        ParentMethod parentMethod = new ParentMethod();
        parentMethod.setSignature(methodSignature);
        parentMethod.setName(method.name());
        if(method.isAbstract() || method.isNative() || method.isStaticInitializer()) {
            parentMethod.setParameters(new BaseType[0]);
        } else {
            try {
                BaseType[] parameters = TypeFactory.create(method.argumentTypes());
                parentMethod.setParameters(parameters);
                parentMethod.setReturnValue(TypeFactory.create(method.returnType()));
            } catch (ClassNotLoadedException classNotLoadedException) {
                log.log(Level.SEVERE, "Type in argument isn't loaded yet: " + method.argumentTypeNames(), classNotLoadedException);
            }
        }
        return parentMethod;
    }


    public ParentMethod getOrCreateMethod(Method method) {
        String className = method.declaringType().name();
        String methodSignature = method.signature();
        ParentClass parentClass = null;
        synchronized (LOCK) {
            parentClass = parentClassMap.get(className);
            if (parentClass == null) {
                parentClass = new ParentClass();
                parentClass.setName(className);
                ParentMethod parentMethod = createMethod(methodSignature, method);
                parentClass.addMethod(parentMethod);
                parentClassMap.put(className, parentClass);
                return parentMethod;
            }
        }

        synchronized (parentClass) {
            ParentMethod parentMethod = parentClass.findMethod(methodSignature);
            if (parentMethod != null) {
                return parentMethod;
            }
            parentMethod = createMethod(methodSignature, method);
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
}
