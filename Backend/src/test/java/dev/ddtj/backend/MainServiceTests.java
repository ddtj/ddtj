package dev.ddtj.backend;

import dev.ddtj.backend.data.Invocation;
import dev.ddtj.backend.data.MockInvocation;
import dev.ddtj.backend.data.ParentClass;
import dev.ddtj.backend.data.ParentMethod;
import dev.ddtj.backend.dto.ClassDTO;
import dev.ddtj.backend.dto.MethodDTO;
import dev.ddtj.backend.dto.TestTimeDTO;
import dev.ddtj.backend.dto.VMDTO;
import dev.ddtj.backend.javadebugger.ConnectSession;
import dev.ddtj.backend.javadebugger.MonitoredSession;
import dev.ddtj.backend.service.MainService;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class MainServiceTests {
    @Mock
    private ConnectSession connectSession;

    @Mock
    private MonitoredSession monitoredSession;

    @InjectMocks
    private MainService mainService;

    private final VMDTO vmDTO = new VMDTO();

    private final ParentClass PARENT_CLASS = new ParentClass();
    private final ParentMethod PARENT_METHOD = new ParentMethod();
    private final List<ParentClass> CLASS_LIST = List.of(PARENT_CLASS);
    private final Invocation invocation = new Invocation();
    private final MockInvocation mockInvocation = new MockInvocation();


    @Test
    void testAPI() throws IOException {
        mainService.setSession(monitoredSession);
        Mockito.when(monitoredSession.listClasses()).thenReturn(CLASS_LIST);
        PARENT_METHOD.setSignature("ParentMethod");
        initLombok();

        PARENT_METHOD.addInvocation(invocation);
        PARENT_CLASS.setName("ParentClass");
        PARENT_CLASS.addMethod(PARENT_METHOD);
        mainService.connect(vmDTO);
        Mockito.verify(connectSession, Mockito.times(1))
                .create(vmDTO);

        mainService.setSession(monitoredSession);

        Mockito.when(monitoredSession.getClass(PARENT_CLASS.getName()))
                .thenReturn(PARENT_CLASS);

        List<ClassDTO> classDTOS = mainService.listClasses();
        assertEquals(1, classDTOS.size());
        assertEquals(PARENT_CLASS.getName(), classDTOS.get(0).getName());

        List<MethodDTO> methodDTOS = mainService.listMethods(PARENT_CLASS.getName());
        assertEquals(1, methodDTOS.size());
        assertEquals(PARENT_METHOD.getSignature(), methodDTOS.get(0).getSignature());

        List<TestTimeDTO> testTimeDTOS = mainService.listInvocations(PARENT_CLASS.getName(), PARENT_METHOD.getSignature());
        assertEquals(0, testTimeDTOS.size());
    }

    private void initLombok() {
        invocation.setArguments(new Object[]{" "});
        invocation.setTime(System.currentTimeMillis());
        invocation.addMockInvocation(mockInvocation);
        invocation.setId("1");
        invocation.setResult(new Object());
        invocation.setThrownException("");

        mockInvocation.setParentMethod(PARENT_METHOD);
        mockInvocation.setArguments(invocation.getArguments());
        mockInvocation.setReturnValue(invocation.getResult());
        mockInvocation.setParentClass(PARENT_CLASS);
    }
}
