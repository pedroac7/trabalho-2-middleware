package repositorio;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainConfigurationTest {

    @Test
    void shouldKeepFixedRepositorioWorkerThreadsInCode() throws Exception {
        Field workersField = Main.class.getDeclaredField("REPOSITORIO_WORKER_THREADS");
        workersField.setAccessible(true);

        assertTrue(Modifier.isStatic(workersField.getModifiers()));
        assertTrue(Modifier.isFinal(workersField.getModifiers()));
        assertEquals(100, workersField.getInt(null));
    }

    @Test
    void shouldKeepFixedRepositorioQueueCapacityInCode() throws Exception {
        Field queueField = Main.class.getDeclaredField("REPOSITORIO_QUEUE_CAPACITY");
        queueField.setAccessible(true);

        assertTrue(Modifier.isStatic(queueField.getModifiers()));
        assertTrue(Modifier.isFinal(queueField.getModifiers()));
        assertEquals(300, queueField.getInt(null));
    }
}
