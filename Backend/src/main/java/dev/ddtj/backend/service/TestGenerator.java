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
package dev.ddtj.backend.service;

import dev.ddtj.backend.data.Invocation;
import dev.ddtj.backend.data.ParentClass;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.data.objectmodel.BaseType;
import dev.ddtj.backend.data.objectmodel.ObjectType;
import dev.ddtj.backend.data.objectmodel.PrimitiveAndWrapperType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TestGenerator {
    private final ParentClass parentClass;
    private final ParentMethod parentMethod;
    private final Invocation invocation;
    private final List<Invocation> internalCalls;

    public TestGenerator(ParentClass parentClass, ParentMethod parentMethod, Invocation invocation, List<Invocation> internalCalls) {
        this.parentClass = parentClass;
        this.parentMethod = parentMethod;
        this.invocation = invocation;
        this.internalCalls = internalCalls;
    }

    public List<String> getCreationCode() {
        ObjectType parentObjectType = parentClass.getObjectType();
        Map<String, Object> fieldValues = new HashMap<>();
        for(int iter = 0 ; iter < invocation.getFields().length ; iter++) {
            fieldValues.put(parentObjectType.getFieldName(iter), invocation.getFields()[iter]);
        }
        return parentObjectType.getCodePrefix("myObjectInstance", fieldValues);
    }

    public Set<String> getCustomImports() {
        Set<String> imports = internalCalls.stream().map(invocation -> invocation.getStack()[0].getParentClass().getName())
                .collect(Collectors.toSet());
        imports.addAll(mockedClassSet());
        return imports;
    }

    private Set<String> mockedClassSet() {
        return internalCalls.stream()
                .map(invocation -> invocation.getStack()[0].getParentClass().getName())
                .collect(Collectors.toSet());
    }

    public List<String> getMocks() {
        List<String> mocks = new ArrayList<>();
        Set<String> mockClasses = mockedClassSet();
        for(String mockClass : mockClasses) {
            String shortName = mockClass.substring(mockClass.lastIndexOf('.') + 1);
            mocks.add(shortName + " " + shortName + "Mock = Mockito.mock(" + shortName + ".class);");
        }
        for(Invocation invocation : internalCalls) {
            ParentMethod method = invocation.getStack()[0];
            if(method.getReturnType() != PrimitiveAndWrapperType.VOID) {
                mocks.addAll(getArgumentInitialization(method, invocation.getArguments()));
                method.getReturnType().getCodePrefix(method.getName() + "ReturnValue", invocation.getResult());
                String shortName = method.getParentClass().getName();
                shortName = shortName.substring(shortName.lastIndexOf('.') + 1);
                mocks.add("Mockito.lenient().when(" + shortName + "Mock." + invocation.getStack()[0].getName() + "(" +
                        getArguments(method, invocation.getArguments()) + ")).thenReturn(" +
                        method.getReturnType().getCodeRepresentation("", invocation.getResult()) + ");");
            }
        }
        return mocks;
    }

    private List<String> getArgumentInitialization(ParentMethod parentMethod, Object[] arguments) {
        BaseType[] baseTypes = parentMethod.getParameters();
        List<String> returnValue = new ArrayList<>();
        for(int iter = 0 ; iter < baseTypes.length ; iter++) {
            BaseType type = baseTypes[iter];
            List<String> initializationCode = type.getCodePrefix(parentMethod.getName() + "Arg" + (iter + 1),
                    arguments[iter]);
            if(initializationCode != null) {
                returnValue.addAll(initializationCode);
            }
        }
        return returnValue;
    }

    public List<String> getArgumentInitialization() {
        return getArgumentInitialization(parentMethod, invocation.getFields());
    }

    private String getArguments(ParentMethod parentMethod, Object[] arguments) {
        BaseType[] baseTypes = parentMethod.getParameters();
        String returnValue = "";
        for(int iter = 0 ; iter < baseTypes.length ; iter++) {
            BaseType type = baseTypes[iter];
            returnValue += type.getCodeRepresentation(parentMethod.getName() + "Arg" + (iter + 1),
                    arguments[iter]);
            if(iter < baseTypes.length - 1) {
                returnValue += ", ";
            }
        }
        return returnValue;
    }

    public String getArguments() {
        return getArguments(parentMethod, invocation.getFields());
    }

    public String getMethodName() {
        return parentMethod.getName();
    }
}
