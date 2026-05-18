package br.ufrn.middleware.client;

import br.ufrn.middleware.error.RemotingException;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.marshaller.InvocationRequestMarshaller;
import br.ufrn.middleware.marshaller.SimpleTextInvocationRequestMarshaller;
import br.ufrn.middleware.marshaller.TextInvocationMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UdpClientRequestHandler implements ClientRequestHandler {
    private static final int DEFAULT_TIMEOUT_MILLIS = 3000;
    private static final int DEFAULT_MAX_PACKET_SIZE = 8192;
    private static final Pattern STATUS_CODE_PATTERN = Pattern.compile("\"statusCode\"\\s*:\\s*(\\d+)");

    private final int timeoutMillis;
    private final int maxPacketSize;
    private final InvocationRequestMarshaller invocationRequestMarshaller;

    public UdpClientRequestHandler() {
        this(DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_PACKET_SIZE, new SimpleTextInvocationRequestMarshaller());
    }

    public UdpClientRequestHandler(int timeoutMillis, int maxPacketSize) {
        this(timeoutMillis, maxPacketSize, new SimpleTextInvocationRequestMarshaller());
    }

    public UdpClientRequestHandler(
            int timeoutMillis,
            int maxPacketSize,
            InvocationRequestMarshaller invocationRequestMarshaller
    ) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be greater than zero.");
        }
        if (maxPacketSize <= 0) {
            throw new IllegalArgumentException("maxPacketSize must be greater than zero.");
        }
        if (invocationRequestMarshaller == null) {
            throw new IllegalArgumentException("InvocationRequestMarshaller must not be null.");
        }
        this.timeoutMillis = timeoutMillis;
        this.maxPacketSize = maxPacketSize;
        this.invocationRequestMarshaller = invocationRequestMarshaller;
    }

    @Override
    public RemoteInvocationResponse send(RemoteInvocationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("RemoteInvocationRequest must not be null.");
        }

        AbsoluteObjectReference reference = request.getObjectReference();
        String protocol = reference.getProtocol();
        if (!"udp".equalsIgnoreCase(protocol)) {
            throw new RemotingException("UdpClientRequestHandler only supports protocol udp.");
        }

        byte[] payload = buildPayload(request);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMillis);

            DatagramPacket requestPacket = new DatagramPacket(
                    payload,
                    payload.length,
                    java.net.InetAddress.getByName(reference.getHost()),
                    reference.getPort()
            );
            socket.send(requestPacket);

            byte[] responseBuffer = new byte[maxPacketSize];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);

            String responseBody = new String(
                    responsePacket.getData(),
                    responsePacket.getOffset(),
                    responsePacket.getLength(),
                    StandardCharsets.UTF_8
            );

            int statusCode = inferStatusCode(responseBody);
            return new RemoteInvocationResponse(statusCode, responseBody);
        } catch (SocketTimeoutException exception) {
            throw new RemotingException("UDP request timed out after " + timeoutMillis + " ms.", exception);
        } catch (IOException exception) {
            throw new RemotingException("Failed to send UDP request.", exception);
        }
    }

    private byte[] buildPayload(RemoteInvocationRequest request) {
        AbsoluteObjectReference reference = request.getObjectReference();
        TextInvocationMessage message = new TextInvocationMessage(
                request.getHttpMethod(),
                buildPath(reference, request.getRemotePath()),
                request.getQueryParams(),
                request.getBody()
        );
        return invocationRequestMarshaller.marshal(message);
    }

    private String buildPath(AbsoluteObjectReference reference, String remotePath) {
        String objectIdPath = "/" + reference.getObjectId().getValue();
        String methodPath = remotePath.startsWith("/") ? remotePath : "/" + remotePath;
        return (objectIdPath + methodPath).replaceAll("/+", "/");
    }

    private int inferStatusCode(String responseBody) {
        Matcher matcher = STATUS_CODE_PATTERN.matcher(responseBody);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        if (responseBody.contains("\"success\":true")) {
            return 200;
        }

        return 500;
    }
}
