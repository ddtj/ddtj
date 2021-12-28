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
package dev.ddtj.cli;

import com.google.gson.Gson;
import dev.ddtj.backend.dto.VMDTO;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "ddtj", mixinStandardHelpOptions = true, version = "0.0.4",
        description = "DDT: It kills bugs", helpCommand = true,
        abbreviateSynopsis = true)
public class Main implements Callable<Integer> {
    private static CommandLine cmd;

    @Option(names = {"-port", "-p"}, description = "The backend server port number (defaults to 2012)")
    private int backendPort = 2012;

    @Option(names = {"-javahome", "-j"}, description = "Location of the Java virtual machine")
    private String javaHome;

    @Option(names = {"-whitelist", "-w"}, description = "Whitelisted packages regular expression (must contain star at start or end)")
    private String whitelist;

    @Option(names = {"-run", "-r"}, description = "Run an application with DDTJ backend")
    private String run;

    @Option(names = {"-list-classes", "-c"}, description = "Display a list of all the classes that were reached")
    private boolean listClasses;

    @Option(names = {"-list-methods", "-m"}, description = "Display a list of methods in the class that were reached")
    private String listMethods;

    @Option(names = {"-list-tests", "-t"}, description = "Display a list of tests in the class.method that were reached")
    private String listTests;

    @Option(names = {"-generate-test", "-g"}, description = "Generate the test for the given test id")
    private String generate;

    @Option(names = {"-classpath", "-cp"}, hidden = true)
    private String classpath;

    @Option(names = {"-jar"}, hidden = true)
    private String jar;

    @Override
    public Integer call() throws Exception {
        String baseUrl = "http://localhost:" + backendPort;
        if(run != null && !run.isBlank()) {
            String arg = "";
            if(classpath != null && !classpath.isEmpty()) {
                arg += "-cp " + classpath;
            }
            if(jar != null && !jar.isEmpty()) {
                arg += " -jar " + jar;
            }
            VMDTO vm = new VMDTO(javaHome, arg, run, whitelist);
            String inputJson = new Gson().toJson(vm);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/connect"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(inputJson)).build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? 0 : response.statusCode();
        }

        if(listClasses) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/classes")).GET()
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
            return 0;
        }

        cmd.usage(System.err);
        return 1;
    }

    public static void main(String[] args) {
        cmd = new CommandLine(new Main());
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
