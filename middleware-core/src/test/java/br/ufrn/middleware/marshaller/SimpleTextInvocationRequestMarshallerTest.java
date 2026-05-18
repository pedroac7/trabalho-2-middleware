package br.ufrn.middleware.marshaller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleTextInvocationRequestMarshallerTest {
    private final SimpleTextInvocationRequestMarshaller marshaller = new SimpleTextInvocationRequestMarshaller();

    @Test
    void marshalGetWithoutBody() {
        TextInvocationMessage message = new TextInvocationMessage(
                "GET",
                "/calculadora/soma",
                Map.of("a", "10", "b", "20"),
                ""
        );

        byte[] bytes = marshaller.marshal(message);
        String serialized = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(serialized.contains("METHOD GET"));
        assertTrue(serialized.contains("PATH /calculadora/soma"));
        assertTrue(serialized.contains("QUERY"));
        assertTrue(serialized.contains("BODY_LENGTH 0"));
        assertTrue(serialized.contains("a=10"));
        assertTrue(serialized.contains("b=20"));
    }

    @Test
    void unmarshalGetWithoutBody() {
        String raw = "METHOD GET\n"
                + "PATH /calculadora/soma\n"
                + "QUERY a=10&b=20\n"
                + "BODY_LENGTH 0\n"
                + "\n";

        TextInvocationMessage message = marshaller.unmarshal(raw.getBytes(StandardCharsets.UTF_8));

        assertEquals("GET", message.getMethod());
        assertEquals("/calculadora/soma", message.getPath());
        assertEquals("10", message.getQueryParams().get("a"));
        assertEquals("20", message.getQueryParams().get("b"));
        assertEquals("", message.getBody());
    }

    @Test
    void marshalPostWithBodyUsesUtf8ByteLength() {
        String body = "conteúdo";
        TextInvocationMessage message = new TextInvocationMessage(
                "POST",
                "/calculadora/body",
                Map.of(),
                body
        );

        byte[] bytes = marshaller.marshal(message);
        String serialized = new String(bytes, StandardCharsets.UTF_8);
        int expectedLength = body.getBytes(StandardCharsets.UTF_8).length;

        assertTrue(serialized.contains("BODY_LENGTH " + expectedLength));
    }

    @Test
    void unmarshalPostWithBody() {
        String body = "conteudo";
        String raw = "METHOD POST\n"
                + "PATH /calculadora/body\n"
                + "QUERY\n"
                + "BODY_LENGTH " + body.getBytes(StandardCharsets.UTF_8).length + "\n"
                + "\n"
                + body;

        TextInvocationMessage message = marshaller.unmarshal(raw.getBytes(StandardCharsets.UTF_8));

        assertEquals("POST", message.getMethod());
        assertEquals("/calculadora/body", message.getPath());
        assertEquals(body, message.getBody());
    }

    @Test
    void unmarshalRejectsMissingMethod() {
        String raw = "PATH /calculadora/soma\n"
                + "QUERY a=10&b=20\n"
                + "BODY_LENGTH 0\n"
                + "\n";

        assertThrows(MarshallingException.class, () -> marshaller.unmarshal(raw.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void unmarshalRejectsInvalidBodyLength() {
        String raw = "METHOD GET\n"
                + "PATH /calculadora/soma\n"
                + "QUERY a=10&b=20\n"
                + "BODY_LENGTH abc\n"
                + "\n";

        assertThrows(MarshallingException.class, () -> marshaller.unmarshal(raw.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void unmarshalRejectsTruncatedBody() {
        String raw = "METHOD POST\n"
                + "PATH /calculadora/body\n"
                + "QUERY\n"
                + "BODY_LENGTH 10\n"
                + "\n"
                + "abc";

        assertThrows(MarshallingException.class, () -> marshaller.unmarshal(raw.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void queryEncodingRoundTrip() {
        TextInvocationMessage source = new TextInvocationMessage(
                "GET",
                "/calculadora/eco",
                Map.of("valor", "ação teste"),
                ""
        );

        byte[] bytes = marshaller.marshal(source);
        TextInvocationMessage decoded = marshaller.unmarshal(bytes);

        assertEquals("ação teste", decoded.getQueryParams().get("valor"));
    }
}
