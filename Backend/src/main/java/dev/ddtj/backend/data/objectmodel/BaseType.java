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

import com.sun.jdi.Value;
import java.lang.reflect.Array;
import java.util.List;

public abstract class BaseType implements BaseTypeInterface, ArrayCreation {
  private final String type;

  protected BaseType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public String getShortTypeName() {
    int index = type.lastIndexOf('.');
    return index == -1 ? type : type.substring(index + 1);
  }

  public void setArrayValue(Object array, int index, Value value) {
    Array.set(array, index, getValue(value));
  }

  public abstract String getCodeRepresentation(String fieldName, Object fieldValue);

  public List<String> getCodePrefix(String fieldName, Object fieldValue) {
    return null;
  }
}
