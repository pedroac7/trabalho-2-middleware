package heartbeat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartbeatReceiverTest {

    @Test
    void shouldParseAnnouncementTypeAndProtocol() {
        HeartbeatReceiver.ServiceAnnouncement validadorHttp =
                HeartbeatReceiver.ServiceAnnouncement.parse("validador|http");
        HeartbeatReceiver.ServiceAnnouncement validadorTcp =
                HeartbeatReceiver.ServiceAnnouncement.parse("validador|tcp");
        HeartbeatReceiver.ServiceAnnouncement repositorioUdp =
                HeartbeatReceiver.ServiceAnnouncement.parse("repositorio|udp");

        assertEquals("validador", validadorHttp.type());
        assertEquals("http", validadorHttp.protocol());
        assertEquals("validador", validadorTcp.type());
        assertEquals("tcp", validadorTcp.protocol());
        assertEquals("repositorio", repositorioUdp.type());
        assertEquals("udp", repositorioUdp.protocol());
    }

    @Test
    void shouldParseServiceInstanceKeyWithProtocol() {
        HeartbeatReceiver.ServiceInstance instance =
                HeartbeatReceiver.ServiceInstance.fromKey("tcp://localhost:8081").orElseThrow();

        assertEquals("tcp", instance.protocol());
        assertEquals("localhost", instance.host());
        assertEquals(8081, instance.port());
        assertEquals("tcp://localhost:8081", instance.toKey());
    }

    @Test
    void shouldKeepLegacyHostPortKeysAsHttp() {
        HeartbeatReceiver.ServiceInstance instance =
                HeartbeatReceiver.ServiceInstance.fromKey("127.0.0.1:8081").orElseThrow();

        assertEquals("http", instance.protocol());
        assertEquals("127.0.0.1", instance.host());
        assertEquals(8081, instance.port());
        assertTrue(HeartbeatReceiver.ServiceInstance.fromKey("invalid").isEmpty());
    }
}
