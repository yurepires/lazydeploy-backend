package com.yurepires.lazydeploy.security.request;

import com.yurepires.lazydeploy.exception.RequestTooLargeException;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

final class RequestBodyTooLargeInputStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private final long maximumBytes;
    private final AtomicLong bytesRead;

    RequestBodyTooLargeInputStream(
            ServletInputStream delegate,
            long maximumBytes,
            AtomicLong bytesRead
    ) {
        this.delegate = delegate;
        this.maximumBytes = maximumBytes;
        this.bytesRead = bytesRead;
    }

    @Override
    public int read() throws IOException {
        int value = delegate.read();
        if (value >= 0) {
            incrementBytesRead(1);
        }
        return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        int read = delegate.read(bytes, offset, length);
        if (read > 0) {
            incrementBytesRead(read);
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean isFinished() {
        return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
        return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        delegate.setReadListener(readListener);
    }

    private void incrementBytesRead(int amount) {
        long totalBytesRead = bytesRead.addAndGet(amount);
        if (totalBytesRead > maximumBytes) {
            throw new RequestTooLargeException();
        }
    }
}
