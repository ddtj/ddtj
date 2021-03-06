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

import com.sun.jdi.ArrayReference;
import com.sun.jdi.Value;
import java.lang.reflect.Array;
import java.util.List;

/**
 * At the moment only 1d arrays are supported. I'm not sure what's the "right approach" to support multidimensional
 * arrays.
 */

public class ArrayObjectOrPrimitiveType extends BaseType {
    private final BaseType elementType;
    protected ArrayObjectOrPrimitiveType(String type, BaseType elementType) {
        super(type);
        this.elementType = elementType;
    }

    @Override
    public Object getValue(Value value) {
        ArrayReference arrayReference = (ArrayReference) value;
        int length = arrayReference.length();
        Object array = allocateArray(length);
        for (int i = 0; i < length; i++) {
            elementType.setArrayValue(array, i, arrayReference.getValue(i));
        }
        return null;
    }

    @Override
    public Object allocateArray(int size) {
        return elementType.allocateArray(size);
    }

    @Override
    public String getCodeRepresentation(String fieldName, Object fieldValue) {
        int arrayLength = Array.getLength(fieldValue);
        if(arrayLength == 0) {
            return "new " + getType() + "[0]";
        }
        if(arrayLength < 10 && !(elementType instanceof ObjectType)) {
            StringBuilder alloc = new StringBuilder("new ").append(getType()).append("[] {");
            for(int i = 0; i < arrayLength; i++) {
                alloc.append(elementType.getCodeRepresentation(fieldName, Array.get(fieldValue, i)));
                if(i < arrayLength - 1) {
                    alloc.append(", ");
                }
            }
            return alloc.append("}").toString();
        }
        return fieldName;
    }

    @Override
    public List<String> getCodePrefix(String fieldName, Object fieldValue) {
        int arrayLength = Array.getLength(fieldValue);
        if(arrayLength == 0 ||
                (arrayLength < 10 && !(elementType instanceof ObjectType))) {
            return null;
        }
        return super.getCodePrefix(fieldName, fieldValue);
    }
}
