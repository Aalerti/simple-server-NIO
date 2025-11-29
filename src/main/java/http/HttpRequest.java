package http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HttpRequest {
    private HttpMethods method;
    private String path;
    private Map<String, String> headers = new HashMap<>();
    private byte[] body;

    public HttpRequest(byte[] bytesRequest) {
        try {
            this.method = parseMethod(bytesRequest);
            this.path = parsePath(bytesRequest);
            parseHeaders(bytesRequest);
            this.body = parseBody(bytesRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpMethods parseMethod(byte[] bytesRequest) throws Exception {
        for (int i = 0; i < bytesRequest.length; i++) {
            if (bytesRequest[i] == 32) {
                return HttpMethods.valueOf(new String(bytesRequest, 0, i, StandardCharsets.UTF_8));
            }
        }
        throw new Exception("Illegal request");
    }

    private String parsePath(byte[] bytesRequest) throws Exception {
        int indexStart = -1;
        for (int i = 0; i < bytesRequest.length; i++) {
            if (bytesRequest[i] == 32 && indexStart == -1) {
                indexStart = i;
            }
            if (bytesRequest[i] == 32 && indexStart != -1 && i != indexStart) {
                return new String(bytesRequest, indexStart + 1, i - (indexStart + 1), StandardCharsets.UTF_8);
            }
        }
        throw new Exception("Illegal Path in Request");
    }

    private byte[] parseBody(byte[] bytesRequest) {
        for (int i = 0; i < bytesRequest.length - 3; i++) {
            if (bytesRequest[i] == 13 && bytesRequest[i+1] == 10
                    && bytesRequest[i+2] == 13 && bytesRequest[i+3] == 10) {
                return Arrays.copyOfRange(bytesRequest, i+4, bytesRequest.length);
            }
        }
        return null;
    }

    private void parseHeaders(byte[] bytesRequest) {
        int splitIndex = -1;
        for (int i = 0; i < bytesRequest.length; i++) {
            if (bytesRequest[i] == 13 && bytesRequest[i+1] == 10 &&
                    bytesRequest[i+2] == 13 && bytesRequest[i+3] == 10) {
                splitIndex = i;
                break;
            }
        }

        if (splitIndex != -1) {
            String headersString = new String(bytesRequest, 0, splitIndex, StandardCharsets.UTF_8);
            String[] lines = headersString.split("\r\n");

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                int separator = line.indexOf(":");
                if (separator != -1) {
                    String key = line.substring(0, separator).trim();
                    String value = line.substring(separator+1).trim();
                    headers.put(key, value);
                }
            }
        }
    }

    public HttpMethods getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public byte[] getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
