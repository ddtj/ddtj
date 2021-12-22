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
package dev.ddtj.backend.rest;

import dev.ddtj.backend.dto.ClassDTO;
import dev.ddtj.backend.dto.MethodDTO;
import dev.ddtj.backend.dto.TestTimeDTO;
import dev.ddtj.backend.javadebugger.ConnectSession;
import dev.ddtj.backend.dto.VMDTO;
import dev.ddtj.backend.javadebugger.MonitoredSession;
import dev.ddtj.backend.service.MainService;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainWS {
    private final MainService mainService;

    public MainWS(ConnectSession connectSession, MainService mainService) {
        this.mainService = mainService;
    }

    @PostMapping("/connect")
    public void connect(@RequestBody VMDTO vmDTO) throws IOException {
        mainService.connect(vmDTO);
    }

    @GetMapping("/classes")
    public List<ClassDTO> listClasses() {
        return mainService.listClasses();
    }

    @GetMapping("/methods")
    public List<MethodDTO> listMethods(String className) {
        return mainService.listMethods(className);
    }

    @GetMapping("/invocations")
    public List<TestTimeDTO> listInvocations(String className, String methodSignature) {
        return mainService.listMethods(className, methodSignature);
    }
}
