package dev.ddtj.backend;

import com.sun.jdi.Method;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.javadebugger.DataCollector;
import dev.ddtj.backend.javadebugger.MonitoredSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataCollectorTests {
    @Mock
    private VMDeathEvent vmDeathEvent;

    @Mock
    private MethodEntryEvent methodEntryEvent;

    @Mock
    private StepEvent stepEvent;

    @Mock
    private MonitoredSession monitoredSession;

    @Mock
    private Method method;

    @Mock
    private ParentMethod parentMethod;

    @Test
    void testProcessEvent() {
        DataCollector dataCollector = new DataCollector();
        Assertions.assertTrue(dataCollector.processEvent(monitoredSession, vmDeathEvent, 1));

        Mockito.when(methodEntryEvent.method()).thenReturn(method);
        Mockito.when(monitoredSession.getOrCreateMethod(method)).thenReturn(parentMethod);

        Assertions.assertFalse(dataCollector.processEvent(monitoredSession, methodEntryEvent, 1));
        Mockito.verify(monitoredSession, Mockito.times(1))
                .getOrCreateMethod(method);
    }
}
