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
package dev.ddtj.backend.javadebugger;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.request.MethodEntryRequest;
import java.io.IOException;
import java.util.Map;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

@Component
@Log
public class ConnectSession {
    private final DataCollector collector;

    public ConnectSession(DataCollector collector) {
        this.collector = collector;
    }

    public MonitoredSession create(String vmHome, String vmOptions, String main, String filter) throws IOException {
        try {
            LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
            Map<String, Argument> env = connector.defaultArguments();
            env.get("main").setValue(main);
            if(vmHome != null && !vmHome.isBlank()) {
                env.get("home").setValue(vmHome);
            }
            if(vmOptions != null && !vmOptions.isBlank()) {
                env.get("options").setValue(vmOptions);
            }
            VirtualMachine vm = connector.launch(env);

            if(filter == null || filter.isBlank()) {
                filter = main.substring(main.lastIndexOf('.') + 1) + "*";
            }
            MethodEntryRequest methodEntryRequest = vm.eventRequestManager().createMethodEntryRequest();
            methodEntryRequest.addClassFilter(filter);
            methodEntryRequest.setSuspendPolicy(MethodEntryRequest.SUSPEND_EVENT_THREAD);
            methodEntryRequest.enable();
            MonitoredSession session = new MonitoredSession(vm, filter);
            collector.collect(session);
            return session;
        } catch (IllegalConnectorArgumentsException | VMStartException e) {
            log.severe("Failed to connect to " + main + ": " + e);
            throw new IOException(e);
        }
    }
}
