package gateway;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainConfigurationTest {

    @Test
    void shouldKeepFixedInternalClientTimeoutInCode() throws Exception {
        Field timeoutField = Main.class.getDeclaredField("INTERNAL_CLIENT_TIMEOUT_MILLIS");
        timeoutField.setAccessible(true);

        assertTrue(Modifier.isStatic(timeoutField.getModifiers()));
        assertTrue(Modifier.isFinal(timeoutField.getModifiers()));
        assertEquals(3000, timeoutField.getInt(null));
    }

    @Test
    void shouldKeepFixedGatewayWorkerThreadsInCode() throws Exception {
        Field workersField = Main.class.getDeclaredField("GATEWAY_WORKER_THREADS");
        workersField.setAccessible(true);

        assertTrue(Modifier.isStatic(workersField.getModifiers()));
        assertTrue(Modifier.isFinal(workersField.getModifiers()));
        assertEquals(300, workersField.getInt(null));
    }

    @Test
    void shouldKeepFixedGatewayQueueCapacityInCode() throws Exception {
        Field queueField = Main.class.getDeclaredField("GATEWAY_QUEUE_CAPACITY");
        queueField.setAccessible(true);

        assertTrue(Modifier.isStatic(queueField.getModifiers()));
        assertTrue(Modifier.isFinal(queueField.getModifiers()));
        assertEquals(1000, queueField.getInt(null));
    }
}
