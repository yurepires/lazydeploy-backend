package com.yurepires.lazydeploy.security.ratelimit;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Permite inspecionar o corpo do login e entregá-lo novamente ao controller.
 * O conteúdo existe somente durante o processamento da requisição.
 */
final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    private CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body;
    }

    static CachedBodyHttpServletRequest from(HttpServletRequest request) throws IOException {
        return new CachedBodyHttpServletRequest(
                request,
                request.getInputStream().readAllBytes()
        );
    }

    byte[] body() {
        return body.clone();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        return new ServletInputStream() {

            @Override
            public int read() {
                return input.read();
            }

            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // Leitura síncrona: não há callback assíncrono a registrar.
            }
        };
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
