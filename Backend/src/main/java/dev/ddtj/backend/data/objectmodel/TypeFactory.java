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

import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VoidType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.java.Log;

@Log
public class TypeFactory {
    private static final Map<Type, BaseType> cache = new HashMap<>();
    public static BaseType create(Value value) {
        return create(value.type());
    }

    public static BaseType create(Type t) {
        // Can't use computeIfAbsent because of recursive modification. When adding a new object the internal properties
        // might change the map and computeIfAbsent will fail on that.
        BaseType baseType = cache.get(t);
        if(baseType == null) {
            baseType = createImpl(t);
            cache.put(t, baseType);
        }
        return baseType;
    }

    private static BaseType createImpl(Type t) {
        if(t instanceof VoidType) {
            return PrimitiveAndWrapperType.VOID;
        }
        if(t instanceof PrimitiveType) {
            switch (t.name()) {
                case "boolean":
                    return PrimitiveAndWrapperType.BOOLEAN;
                case "byte":
                    return PrimitiveAndWrapperType.BYTE;
                case "char":
                    return PrimitiveAndWrapperType.CHARACTER;
                case "short":
                    return PrimitiveAndWrapperType.SHORT;
                case "int":
                    return PrimitiveAndWrapperType.INTEGER;
                case "long":
                    return PrimitiveAndWrapperType.LONG;
                case "float":
                    return PrimitiveAndWrapperType.FLOAT;
                case "double":
                    return PrimitiveAndWrapperType.DOUBLE;

                default:
                    throw new IllegalArgumentException("Unsupported primitive type: " + t.name());
            }
        }
        if(t instanceof ArrayType) {
            try {
                return new ArrayObjectOrPrimitiveType(t.name(), create(((ArrayType) t).componentType()));
            } catch (ClassNotLoadedException e) {
                log.log(java.util.logging.Level.SEVERE, "Could not create array type: " +
                        ((ArrayType) t).componentTypeName(), e);
                throw new IllegalStateException(e);
            }
        }
        ReferenceType rt = (ReferenceType) t;
        switch (rt.name()) {
            case "java.lang.String":
                return BuiltinTypes.STRING;

            case "java.lang.Void":
                return PrimitiveAndWrapperType.VOID;

            case "java.lang.Boolean":
                return PrimitiveAndWrapperType.BOOLEAN_WRAPPER;

            case "java.lang.Byte":
                return PrimitiveAndWrapperType.BYTE_WRAPPER;

            case "java.lang.Character":
                return PrimitiveAndWrapperType.CHARACTER_WRAPPER;

            case "java.lang.Short":
                return PrimitiveAndWrapperType.SHORT_WRAPPER;

            case "java.lang.Integer":
                return PrimitiveAndWrapperType.INTEGER_WRAPPER;

            case "java.lang.Long":
                return PrimitiveAndWrapperType.LONG_WRAPPER;

            case "java.lang.Float":
                return PrimitiveAndWrapperType.FLOAT_WRAPPER;

            case "java.lang.Double":
                return PrimitiveAndWrapperType.DOUBLE_WRAPPER;

            // We want to override all of these for future use in the code generation phase
            case "java.lang.StringBuffer":
            case "java.lang.StringBuilder":
            case "java.lang.CharSequence":
            case "java.util.TimeZone":
            case "java.util.Date":
            case "java.util.Calendar":
            case "javax.time.LocalDate":
            case "javax.time.Instant":
            case "java.util.Collection":
            case "java.util.List":
            case "java.util.Map":
            case "java.util.SortedMap":
            case "java.util.SortedSet":
            case "java.util.Set":
            case "java.util.Vector":
            case "java.util.ArrayList":
            default:
                return ObjectType.create(rt);
        }
    }

    public static BaseType[] create(List<Type> types) {
        BaseType[] fieldBaseTypes = new BaseType[types.size()];
        for(int i = 0; i < types.size(); i++) {
            fieldBaseTypes[i] = create(types.get(i));
        }
        return fieldBaseTypes;
    }
}
