package validador;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainConfigurationTest {

    @Test
    void shouldKeepFixedValidadorWorkerThreadsInCode() throws Exception {
        Field workersField = Main.class.getDeclaredField("VALIDADOR_WORKER_THREADS");
        workersField.setAccessible(true);

        assertTrue(Modifier.isStatic(workersField.getModifiers()));
        assertTrue(Modifier.isFinal(workersField.getModifiers()));
        assertEquals(40, workersField.getInt(null));
    }

    @Test
    void shouldKeepFixedValidadorQueueCapacityInCode() throws Exception {
        Field queueField = Main.class.getDeclaredField("VALIDADOR_QUEUE_CAPACITY");
        queueField.setAccessible(true);

        assertTrue(Modifier.isStatic(queueField.getModifiers()));
        assertTrue(Modifier.isFinal(queueField.getModifiers()));
        assertEquals(80, queueField.getInt(null));
    }
}
