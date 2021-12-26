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
package dev.ddtj.backend.data.objectmodel;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import lombok.extern.java.Log;

@Log
public class ObjectType extends BaseType {
    enum CreationType {
        CONSTRUCTOR_FACTORY,
        SETTERS,

        // failed types
        ABSTRACT,
        PRIVATE,
        PACKAGE_PRIVATE,
        NO_VALID_SETTERS,
        NO_VALID_CONSTRUCTOR
    }

    /**
     * Note: for constructor types the order of the fields matches their order in the constructor
     */
    private final String[] supportedFields;
    private final BaseType[] fieldBaseTypes;
    private final CreationType creationType;

    private ObjectType(String type, String[] supportedFields, BaseType[] fieldBaseTypes, CreationType creationType) {
        super(type);
        this.supportedFields = supportedFields;
        this.fieldBaseTypes = fieldBaseTypes;
        this.creationType = creationType;
    }

    public boolean canObjectBeCreated() {
        return supportedFields != null;
    }

    public static ObjectType create(ReferenceType referenceType) {
        if(referenceType.isAbstract()) {
            return new ObjectType(referenceType.name(), null, null, CreationType.ABSTRACT);
        }
        if(referenceType.isPrivate()) {
            return new ObjectType(referenceType.name(), null, null, CreationType.PRIVATE);
        }
        if(referenceType.isPackagePrivate()) {
            return new ObjectType(referenceType.name(), null, null, CreationType.PACKAGE_PRIVATE);
        }
        String type = referenceType.name();
        List<Method> methodList = referenceType.methods();
        List<Field> fieldList = referenceType.allFields().stream().filter(field -> !field.isStatic() &&
                        !field.isTransient() && !field.isSynthetic() && !field.isEnumConstant() && !field.isFinal())
                .collect(java.util.stream.Collectors.toList());
        Optional<Method> fieldConstructor = findFieldConstructor(methodList, fieldList);
        if(fieldConstructor.isPresent()) {
            List<String> fieldNames = fieldConstructor.get().argumentTypeNames();
            try {
                BaseType[] types = TypeFactory.create(fieldConstructor.get().argumentTypes());
                String[] supportedFields = new String[fieldNames.size()];
                fieldNames.toArray(supportedFields);
                return new ObjectType(type, supportedFields, types, CreationType.CONSTRUCTOR_FACTORY);
            } catch (ClassNotLoadedException e) {
                log.log(Level.SEVERE, "Could not load class: " + fieldConstructor.get(), e);
            }
        }
        Optional<Method> defaultConstructor = findDefaultConstructor(methodList);
        if(defaultConstructor.isPresent()) {
            List<Field> setters = findSetterFields(methodList, fieldList);
            if(setters.size() >= fieldList.size() / 2) {
                try {
                    String[] settersArray = setters.stream().map(Field::name).toArray(String[]::new);
                    List<Type> setterTypes = new ArrayList<>();
                    for (Field setter : setters) {
                        setterTypes.add(setter.type());
                    }
                    return new ObjectType(type, settersArray, TypeFactory.create(setterTypes), CreationType.SETTERS);
                } catch (ClassNotLoadedException e) {
                    log.log(Level.SEVERE, "Could not load class: " + setters.get(0), e);
                }
            }
            return new ObjectType(type, null, null, CreationType.NO_VALID_SETTERS);
        }
        return new ObjectType(type, null, null, CreationType.NO_VALID_CONSTRUCTOR);
    }

    private static List<Field> findSetterFields(List<Method> methodList, List<Field> fieldList) {
        List<Field> setters = new ArrayList<>();
        for(Field field : fieldList) {
            Optional<Method> setter = methodList.stream().filter(method ->
                    (method.name().equalsIgnoreCase("set" + field.name()) ||
                            method.name().equalsIgnoreCase(field.name())) &&
                            method.argumentTypeNames().size() == 1 &&
                            method.argumentTypeNames().get(0).equals(field.typeName()) &&
                            method.isPublic()).findFirst();
            setter.ifPresent(method -> setters.add(field));
        }
        return setters;
    }


    private static Optional<Method> findFieldConstructor(List<Method> methodList, List<Field> fieldList) {
        // I'm making the assumption that the largest constructor will initialize all the fields. This might be wrong
        // in reality
        Method largestConstructor = null;
        int argumentCount = 0;
        for(Method method : methodList) {
            if (method.name().equals("<init>") && method.isPublic() &&
                    method.argumentTypeNames().size() > argumentCount) {
                largestConstructor = method;
                argumentCount = method.argumentTypeNames().size();
            }
        }
        if(largestConstructor != null) {
            // validate the constructor matches field names/types
            List<Field> foundFieldsInOrder = findFieldsForConstructor(largestConstructor, fieldList);
            if(foundFieldsInOrder.size() >= fieldList.size() / 2) {
                return Optional.of(largestConstructor);
            }
        }
        return Optional.empty();
    }

    private static List<Field> findFieldsForConstructor(Method constructor, List<Field> fieldList) {
        List<Field> fields = new ArrayList<>();
        try {
            List<LocalVariable> arguments = constructor.arguments();
            for(LocalVariable arg : arguments) {
                String argName = arg.name();
                Optional<Field> matchingField = fieldList.stream().filter(field ->
                        field.name().equalsIgnoreCase(argName))
                        .findFirst();
                matchingField.ifPresent(field -> {
                    if(arg.typeName().equals(field.typeName())) {
                        fields.add(field);
                    }
                });
            }
        } catch (AbsentInformationException e) {
            log.log(Level.WARNING, "Argument information is missing for constructor " + constructor, e);
        }
        return fields;
    }

    private static Optional<Method> findDefaultConstructor(List<Method> methodList) {
        return methodList.stream().filter(method ->
                        method.name().equals("<init>") && method.isPublic() && method.argumentTypeNames().size() == 0)
                .findFirst();
    }

    @Override
    public Object getValue(Value value) {
        Map<String, Object> fieldValues = new HashMap<>();
        ObjectReference objectReference = (ObjectReference) value;
        for(int iter = 0 ; iter < supportedFields.length ; iter++) {
            Value v = objectReference.getValue(objectReference.referenceType().fieldByName(supportedFields[iter]));
            fieldValues.put(supportedFields[iter], fieldBaseTypes[iter].getValue(v));
        }
        fieldValues.put("class", getType());
        return fieldValues;
    }

    @Override
    public Object allocateArray(int size) {
        return new Map[size];
    }
}
