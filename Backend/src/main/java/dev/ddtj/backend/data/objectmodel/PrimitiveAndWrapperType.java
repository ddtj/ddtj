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

import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.ShortValue;
import com.sun.jdi.Value;

/**
 * Note: primitive type also represents object wrapper types!
 */

public class PrimitiveAndWrapperType extends BaseType {
    public static final PrimitiveAndWrapperType VOID = new PrimitiveAndWrapperType("Void", true,
            v -> { throw new UnsupportedOperationException("void type"); },
            v -> null);

    public static final PrimitiveAndWrapperType BOOLEAN = new PrimitiveAndWrapperType("boolean", false,
            v -> v instanceof BooleanValue ? ((BooleanValue)v).value() : ((ByteValue)v).booleanValue(),
            boolean[]::new) {
        @Override
        public void setArrayValue(Object array, int index, Value value) {
            ((boolean[])array)[index] = ((BooleanValue)value).value();
        }
    };

    public static final PrimitiveAndWrapperType BOOLEAN_WRAPPER = new PrimitiveAndWrapperType(Boolean.class.getName(),
            true, v -> v instanceof BooleanValue ? ((BooleanValue)v).value() : ((ByteValue)v).booleanValue(),
            Boolean[]::new);

    public static final PrimitiveAndWrapperType BYTE = new PrimitiveAndWrapperType("byte", false,
            v -> ((ByteValue)v).value(),
            byte[]::new) {
        @Override
        public void setArrayValue(Object array, int index, Value value) {
            ((byte[])array)[index] = ((ByteValue)value).value();
        }
    };

    public static final PrimitiveAndWrapperType BYTE_WRAPPER = new PrimitiveAndWrapperType(Byte.class.getName(),
            true, v -> ((ByteValue)v).value(),
            Byte[]::new);

    public static final PrimitiveAndWrapperType CHARACTER = new PrimitiveAndWrapperType("char", false,
            v -> ((CharValue)v).value(),
            char[]::new) {
        @Override
        public void setArrayValue(Object array, int index, Value value) {
            ((char[])array)[index] = ((CharValue)value).value();
        }

        @Override
        public String getCodeRepresentation(String fieldName, Object fieldValue) {
            return "'" + fieldValue + "'";
        }
    };

    public static final PrimitiveAndWrapperType CHARACTER_WRAPPER = new PrimitiveAndWrapperType(Character.class.getName(),
            true, v -> ((CharValue)v).value(),
            Character[]::new) {

        @Override
        public String getCodeRepresentation(String fieldName, Object fieldValue) {
            return "'" + fieldValue + "'";
        }
    };

    public static final PrimitiveAndWrapperType SHORT = new PrimitiveAndWrapperType("short", false,
            v -> ((ShortValue)v).value(),
            short[]::new) {
        @Override
        public void setArrayValue(Object array, int index, Value value) {
            ((short[])array)[index] = ((ShortValue)value).value();
        }
    };

    public static final PrimitiveAndWrapperType SHORT_WRAPPER = new PrimitiveAndWrapperType(Short.class.getName(),
            true, v -> ((ShortValue)v).value(),
            Short[]::new);

    public static final PrimitiveAndWrapperType INTEGER = new PrimitiveAndWrapperType("int", false,
            v -> ((IntegerValue)v).value(),
            int[]::new) {
        @Override
        public void setArrayValue(Object array, int index, Value value) {
            ((int[])array)[index] = ((IntegerValue)value).value();
        }
    };

    public static final PrimitiveAndWrapperType INTEGER_WRAPPER = new PrimitiveAndWrapperType(Integer.class.getName(),
            true, v -> ((IntegerValue)v).value(),
            Integer[]::new);

    public static final PrimitiveAndWrapperType LONG = new PrimitiveAndWrapperType("long", false,
            v -> ((LongValue)v).value(),
            long[]::new) {
        @Override
        public void setArrayValue(Object array, int index, Value value) {
            ((long[])array)[index] = ((LongValue)value).value();
        }

        @Override
        public String getCodeRepresentation(String fieldName, Object fieldValue) {
            return fieldValue + "L";
        }
    };

    public static final PrimitiveAndWrapperType LONG_WRAPPER = new PrimitiveAndWrapperType(Long.class.getName(),
            true, v -> ((LongValue)v).value(),
            Long[]::new) {

        @Override
        public String getCodeRepresentation(String fieldName, Object fieldValue) {
            return fieldValue + "L";
        }
    };

    public static final PrimitiveAndWrapperType FLOAT = new PrimitiveAndWrapperType("float", false,
            v -> ((FloatValue)v).value(),
            float[]::new) {
        @Override
        public void setArrayValue(Object array, int index, Value value) {
            ((float[])array)[index] = ((FloatValue)value).value();
        }

        @Override
        public String getCodeRepresentation(String fieldName, Object fieldValue) {
            return fieldValue + "f";
        }
    };

    public static final PrimitiveAndWrapperType FLOAT_WRAPPER = new PrimitiveAndWrapperType(Float.class.getName(),
            true, v -> ((FloatValue)v).value(),
            Float[]::new) {
        @Override
        public String getCodeRepresentation(String fieldName, Object fieldValue) {
            return fieldValue + "L";
        }
    };

    public static final PrimitiveAndWrapperType DOUBLE = new PrimitiveAndWrapperType("double", false,
            v -> ((DoubleValue)v).value(),
            double[]::new) {
        @Override
        public void setArrayValue(Object array, int index, Value value) {
            ((double[])array)[index] = ((DoubleValue)value).value();
        }
    };

    public static final PrimitiveAndWrapperType DOUBLE_WRAPPER = new PrimitiveAndWrapperType(Double.class.getName(),
            true, v -> ((DoubleValue)v).value(),
            Double[]::new);

    private final boolean wrapper;
    private final BaseTypeInterface value;
    private final ArrayCreation arrayCreation;
    protected PrimitiveAndWrapperType(String name, boolean wrapper, BaseTypeInterface value, ArrayCreation arrayCreation) {
        super(name);
        this.wrapper = wrapper;
        this.value = value;
        this.arrayCreation = arrayCreation;
    }

    public boolean isWrapper() {
        return wrapper;
    }

    @Override
    public Object getValue(Value value) {
        return this.value.getValue(value);
    }

    @Override
    public Object allocateArray(int size) {
        return arrayCreation.allocateArray(size);
    }

    @Override
    public String getCodeRepresentation(String fieldName, Object fieldValue) {
        return fieldValue.toString();
    }
}
