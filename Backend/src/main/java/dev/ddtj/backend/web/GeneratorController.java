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
package dev.ddtj.backend.web;

import dev.ddtj.backend.service.MainService;
import dev.ddtj.backend.service.TestGenerator;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class GeneratorController {
    private final MainService mainService;

    public GeneratorController(MainService mainService) {
        this.mainService = mainService;
    }

    @GetMapping("/generate")
    public ModelAndView generateTest(String className, String method, String testId) {
        TestGenerator generator = mainService.generateTest(className, method, testId);

        Map<String, Object> model = new HashMap<>();
        model.put("packageName", className.substring(0, className.lastIndexOf('.')) + ".test");
        model.put("classFullName", className);
        model.put("className", className.substring(className.lastIndexOf('.') + 1));

        model.put("customImports", generator.getCustomImports());
        model.put("creationCode", generator.getCreationCode());
        model.put("methodName", generator.getMethodName());
        model.put("mocks", generator.getMocks());
        model.put("argumentInitialization", generator.getArgumentInitialization());
        model.put("arguments", generator.getArguments());

        return new ModelAndView("java-junit5-mockito", model);
    }

}
