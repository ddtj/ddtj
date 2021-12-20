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

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Method;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.request.StepRequest;
import dev.ddtj.backend.data.Invocation;
import dev.ddtj.backend.data.ParentMethod;
import java.util.logging.Level;
import java.util.stream.Collectors;
import lombok.extern.java.Log;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Log
public class DataCollector {
    private static final String METHOD_PARENT_KEY = "methodParent";
    private static final String INVOCATION_KEY = "invocation";

    @Async
    public void collect(MonitoredSession session) {
        try {
            EventSet eventSet = session.getVirtualMachine().eventQueue().remove(100);
            while (eventSet != null) {
                for (Event event : eventSet) {
                    if (processEvent(session, event)) {
                        return;
                    }
                }
                session.getVirtualMachine().resume();
                eventSet = session.getVirtualMachine().eventQueue().remove(100);
            }
        } catch (InterruptedException e) {
            log.log(Level.SEVERE,"Interrupted while waiting for event queue", e);

            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }

    public boolean processEvent(MonitoredSession session, Event event) {
        if(event instanceof VMDeathEvent) {
            return true;
        }
        if(event instanceof MethodEntryEvent) {
            MethodEntryEvent methodEntryEvent = (MethodEntryEvent) event;

            ParentMethod parent = session.enteringMethod(methodEntryEvent.method());
            Invocation invocation = new Invocation();
            invocation.setTime(System.currentTimeMillis());
            /*try {
                invocation.setArguments(methodEntryEvent.thread().frame(0).getArgumentValues()
                        .stream().map(arg -> arg.).collect(Collectors.toList()).toArray());
            } catch (IncompatibleThreadStateException e) {
                invocation.setArguments(new Object[0]);
            }

            StepRequest stepRequest = event.virtualMachine().eventRequestManager().createStepRequest(
                    methodEntryEvent.thread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER);
            stepRequest.putProperty(METHOD_PARENT_KEY, parent);
            stepRequest.enable();*/
        }
        return false;
    }
}
