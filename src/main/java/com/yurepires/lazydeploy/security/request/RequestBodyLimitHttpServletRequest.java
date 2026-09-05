package com.yurepires.lazydeploy.security.request;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

final class RequestBodyLimitHttpServletRequest extends HttpServletRequestWrapper {

    private final long maximumBytes;
    private final AtomicLong bytesRead = new AtomicLong();

    RequestBodyLimitHttpServletRequest(
            HttpServletRequest request,
            long maximumBytes
    ) {
        super(request);
        this.maximumBytes = maximumBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new RequestBodyTooLargeInputStream(
                super.getInputStream(),
                maximumBytes,
                bytesRead
        );
    }

    @Override
    public BufferedReader getReader() throws IOException {
        Charset charset = resolveCharset();
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    private Charset resolveCharset() {
        String encoding = getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(encoding);
    }
}
