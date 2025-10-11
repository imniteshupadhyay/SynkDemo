package com.playmotech.api.core.filters;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class LoggingResponseWrapper extends HttpServletResponseWrapper {

	private final CaptureServletOutputStream captureStream = new CaptureServletOutputStream();
	private final PrintWriter writer = new PrintWriter(captureStream);

	public LoggingResponseWrapper(HttpServletResponse response) {
		super(response);
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return writer;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return captureStream;
	}

	@Override
	public void flushBuffer() throws IOException {
		super.flushBuffer();
		captureStream.flush();
	}

	@Override
	public void reset() {
		super.reset();
		captureStream.reset();
	}

	@Override
	public void setContentLength(int len) {
		// Set content length if needed
	}

	public String getCapturedResponseBody() {
		return captureStream.getCapturedData();
	}

	private static class CaptureServletOutputStream extends ServletOutputStream {
		private final StringWriter internalWriter = new StringWriter();

		@Override
		public void write(int b) throws IOException {
			internalWriter.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			internalWriter.write(new String(b, off, len));
		}

		@Override
		public void flush() throws IOException {
			internalWriter.flush();
		}

		@Override
		public void close() throws IOException {
			internalWriter.close();
		}

		public String getCapturedData() {
			return internalWriter.toString();
		}

		public void reset() {
			internalWriter.getBuffer().setLength(0);
		}

		@Override
		public boolean isReady() {
			return false;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {

		}
	}
}
