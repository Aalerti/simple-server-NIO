package http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class HttpResponse {
    private String status;
    private String contentType;
    private int contentLength;
    private byte[] body;

    public HttpResponse(String status, String contentType, String message) {
        this(status, contentType, message.getBytes(StandardCharsets.UTF_8));
    }

    public HttpResponse(String status, String contentType, byte[] body) {
        this.status = status;
        this.contentType = contentType;
        this.contentLength = body.length;
        this.body = body;
    }

    public byte[] getBytes() {
        StringBuilder headers = new StringBuilder();
        headers.append("HTTP/1.1 ").append(this.status).append("\r\n");
        headers.append("Content-Type: ").append(this.contentType).append("\r\n");
        headers.append("Content-Length: ").append(this.contentLength).append("\r\n");
        headers.append("\r\n");

        byte[] headersBytes = headers.toString().getBytes(StandardCharsets.UTF_8);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(headersBytes);
            outputStream.write(this.body);

            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
