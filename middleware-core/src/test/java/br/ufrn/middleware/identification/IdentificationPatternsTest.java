package br.ufrn.middleware.identification;

import br.ufrn.middleware.annotations.Param;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.registry.RemoteMethodDescriptor;
import br.ufrn.middleware.registry.RemoteObjectDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentificationPatternsTest {

    @RemoteComponent("calculadora")
    static class CalculadoraService {
        @RemoteMethod(method = "GET", path = "/soma")
        public int soma(@Param("a") int a, @Param("b") int b) {
            return a + b;
        }
    }

    @Test
    void objectIdNormalizesAndComparesByValue() {
        ObjectId left = ObjectId.of(" calculadora ");
        ObjectId right = ObjectId.of("calculadora");

        assertEquals(left, right);
    }

    @Test
    void absoluteObjectReferenceBuildsUri() {
        AbsoluteObjectReference reference = new AbsoluteObjectReference(
                "http",
                "localhost",
                8080,
                ObjectId.of("calculadora")
        );

        assertEquals("http://localhost:8080/calculadora", reference.toUri().toString());
        assertEquals("/calculadora", reference.toPath());
    }

    @Test
    void absoluteObjectReferenceParsesUri() {
        AbsoluteObjectReference reference = AbsoluteObjectReference.fromUri("http://localhost:8080/calculadora");

        assertEquals("http", reference.getProtocol());
        assertEquals("localhost", reference.getHost());
        assertEquals(8080, reference.getPort());
        assertEquals(ObjectId.of("calculadora"), reference.getObjectId());
    }

    @Test
    void localLookupFindsRegisteredComponent() {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());

        Optional<RemoteObjectDescriptor> descriptor = middleware.getLookup().lookup(ObjectId.of("calculadora"));

        assertTrue(descriptor.isPresent());
    }

    @Test
    void localLookupFindsRegisteredMethod() {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());

        Optional<RemoteMethodDescriptor> methodDescriptor =
                middleware.getLookup().lookupMethod("GET", "/calculadora/soma");

        assertTrue(methodDescriptor.isPresent());
    }

    @Test
    void descriptorsExposeObjectId() {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());

        RemoteObjectDescriptor componentDescriptor = middleware.getRegistry()
                .findComponent("calculadora")
                .orElseThrow();
        RemoteMethodDescriptor methodDescriptor = middleware.getRegistry()
                .findMethod("GET", "/calculadora/soma")
                .orElseThrow();

        assertEquals(ObjectId.of("calculadora"), componentDescriptor.getObjectId());
        assertEquals(ObjectId.of("calculadora"), methodDescriptor.getObjectId());
    }
}
