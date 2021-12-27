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

import dev.ddtj.backend.data.ParentClass;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.dto.ClassDTO;
import dev.ddtj.backend.dto.MethodDTO;
import dev.ddtj.backend.dto.TestTimeDTO;
import dev.ddtj.backend.dto.VMDTO;
import dev.ddtj.backend.javadebugger.ConnectSession;
import dev.ddtj.backend.javadebugger.MonitoredSession;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MainService {
    private final ConnectSession connectSession;

    /**
     * We don't need clustering and we don't need to use a database.
     * I might replace this with a map to support concurrently running multiple apps
     * but for now just storing the current session as a field will do.
     */
    private MonitoredSession session;

    public MainService(ConnectSession connectSession) {
        this.connectSession = connectSession;
    }

    public void connect(VMDTO vmDTO) throws IOException {
        session = connectSession.create(vmDTO);
    }

    /**
     * This is a bit of a hack for the unit tests
     * @deprecated this is for internal unit tests only
     */
    @Deprecated
    public void setSession(MonitoredSession session) {
        this.session = session;
    }

    public List<ClassDTO> listClasses() {
        // TODO: This is a bit inefficient filtering should happen first and counting should be beforehand
        return session.listClasses().stream()
                .map(parentClass -> {
                    int methodCount = listMethods(parentClass.getName()).size();
                    ClassDTO classDTO = new ClassDTO();
                    classDTO.setName(parentClass.getName());
                    classDTO.setMethods(methodCount);
                    classDTO.setTotalExecutions(parentClass.countTotalExecutions());
                    return classDTO;
                })
                .filter(c -> c.getMethods() > 0)
                .collect(Collectors.toList());
    }

    public List<MethodDTO> listMethods(String className) {
        // This will be null if it's missing and fail on a null pointer exception
        // we'll need a better error message
        ParentClass parentClass = session.getClass(className);
        List<ParentMethod> methods = parentClass.listMethods();
        return methods.stream()
                .filter(ParentMethod::isApplicable)
                .map(parentMethod -> {
                    MethodDTO methodDTO = new MethodDTO();
                    methodDTO.setFullName(parentMethod.fullName());
                    methodDTO.setTotalExecutions(parentMethod.getInvocationCount());
                    return methodDTO;
                }).collect(Collectors.toList());

    }

    public List<TestTimeDTO> listInvocations(String className, String methodFullName) {
        ParentClass parentClass = session.getClass(className);
        ParentMethod method = parentClass.findMethod(methodFullName);
        return method.listInvocations().stream().map(invocation -> {
            TestTimeDTO testTimeDTO = new TestTimeDTO();
            testTimeDTO.setTime(invocation.getTime());
            testTimeDTO.setId(invocation.getId());
            return testTimeDTO;
        }).collect(Collectors.toList());
    }
}
