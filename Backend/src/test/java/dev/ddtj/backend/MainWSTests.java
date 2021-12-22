package dev.ddtj.backend;

import dev.ddtj.backend.dto.VMDTO;
import dev.ddtj.backend.rest.MainWS;
import dev.ddtj.backend.service.MainService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class MainWSTests {
    @Mock
    private MainService mainService;

    @InjectMocks
    private MainWS mainWS;

    private final VMDTO vmDTO = new VMDTO();

    @Test
    public void testConnectTest() throws IOException {
        mainWS.connect(vmDTO);
        Mockito.verify(mainService).connect(vmDTO);
    }

    @Test
    public void listClassesTest() {
        mainWS.listClasses();
        Mockito.verify(mainService).listClasses();
    }

    @Test
    public void listMethods() {
        mainWS.listMethods(getClass().getName());
        Mockito.verify(mainService).listMethods(getClass().getName());
    }

    @Test
    public void listInvocations() {
        mainWS.listInvocations(getClass().getName(), "sig");
        Mockito.verify(mainService).listInvocations(getClass().getName(), "sig");
    }
}
