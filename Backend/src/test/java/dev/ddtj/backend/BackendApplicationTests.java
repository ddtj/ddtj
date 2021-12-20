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
package dev.ddtj.backend;

import com.sun.jdi.event.EventSet;
import dev.ddtj.backend.javadebugger.DataCollector;
import dev.ddtj.backend.javadebugger.ConnectSession;
import dev.ddtj.backend.javadebugger.MonitoredSession;
import dev.ddtj.backend.testdata.HelloWorld;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(MockitoExtension.class)
class BackendApplicationTests {
	@Mock
	private DataCollector dataCollector;

	@InjectMocks
	@Autowired
	private ConnectSession connectSession;

	@Test
	void testConnectSessionCreate() throws Exception {
		MonitoredSession monitoredSession = connectSession.create(null,
				null, HelloWorld.class.getName(), null);

		EventSet eventSet = monitoredSession.getVirtualMachine().eventQueue().remove(100);
		Assertions.assertThat(eventSet).isNotNull();

		Mockito.verify(dataCollector, Mockito.times(1))
				.collect(monitoredSession);
	}
}
