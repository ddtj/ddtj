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

import com.sun.jdi.StringReference;
import com.sun.jdi.Value;

/**
 * Support for common builtin types to make their handling and code generation feel smoother
 */
public abstract class BuiltinTypes extends BaseType {
    private final BaseTypeInterface value;
    private final ArrayCreation arrayCreation;
    public static final BuiltinTypes STRING = new BuiltinTypes(String.class.getName(), val -> ((StringReference) val).value(),
            String[]::new) {
        @Override
        public String getCodeRepresentation(String fieldName, Object fieldValue) {
            return "\"" + fieldValue + "\"";
        }
    };


    private BuiltinTypes(String type, BaseTypeInterface value, ArrayCreation arrayCreation) {
        super(type);
        this.value = value;
        this.arrayCreation = arrayCreation;
    }

    @Override
    public Object getValue(Value value) {
        return this.value.getValue(value);
    }

    @Override
    public Object allocateArray(int size) {
        return arrayCreation.allocateArray(size);
    }
}
