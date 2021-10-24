package io.github.albertus82.jface.console;

import static io.github.albertus82.jface.DisplayThreadExecutor.Mode.ASYNC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scrollable;

import io.github.albertus82.jface.DisplayThreadExecutor;
import io.github.albertus82.jface.JFaceMessages;
import io.github.albertus82.util.ISupplier;
import io.github.albertus82.util.NewLine;
import io.github.albertus82.util.Supplier;
import io.github.albertus82.util.logging.LoggerFactory;
import io.github.albertus82.util.logging.LoggingSupport;

public abstract class ScrollableConsole<T extends Scrollable> extends OutputStream {

	private static final Logger log = LoggerFactory.getLogger(ScrollableConsole.class);

	protected static final PrintStream defaultSysOut = System.out; // NOSONAR
	protected static final PrintStream defaultSysErr = System.err; // NOSONAR
	protected static final String newLine = NewLine.SYSTEM_LINE_SEPARATOR;

	protected final boolean redirectSystemStream;
	protected final T scrollable;
	protected ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

	private ISupplier<Integer> limit;

	protected ScrollableConsole(final Composite parent, final Object layoutData, final boolean redirectSystemStream) {
		this.redirectSystemStream = redirectSystemStream;

		scrollable = createScrollable(parent);
		scrollable.setLayoutData(layoutData);
		scrollable.setFont(JFaceResources.getTextFont());

		if (redirectSystemStream) {
			scrollable.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(final DisposeEvent de) {
					resetStreams();
				}
			});
			redirectStreams(); // SystemConsole & System.out will print on this SWT Text Widget.
		}
	}

	protected abstract T createScrollable(Composite parent);

	protected abstract void doPrint(String value, int limit);

	public abstract void clear();

	public abstract boolean isEmpty();

	public abstract boolean hasSelection();

	@Override
	public void write(final int b) throws IOException {
		ensureOpen();
		byteBuffer.write(b); // The wrapping PrintStream will flush automatically when needed.
	}

	@Override
	public void flush() throws IOException {
		ensureOpen();
		if (byteBuffer.size() != 0) {
			print(byteBuffer.toString()); // platform's default character set
			byteBuffer.reset();
		}
	}

	@Override
	public void close() throws IOException {
		if (byteBuffer != null) {
			flush();
			if (redirectSystemStream) {
				resetStreams();
			}
			byteBuffer = null;
		}
	}

	protected void print(final String value) {
		// Dealing with null argument...
		final String toPrint;
		if (value == null) {
			toPrint = String.valueOf(value);
		}
		else {
			toPrint = value;
		}

		final int capacity = getLimit();

		// Actual print... (async avoids deadlocks)
		new DisplayThreadExecutor(scrollable, ASYNC) {
			@Override
			protected void onError(final Exception exception) {
				failSafePrint(toPrint);
			}
		}.execute(new Runnable() {
			@Override
			public void run() {
				doPrint(toPrint, capacity);
			}
		});
	}

	protected void failSafePrint(final String value) {
		defaultSysOut.print(value);
	}

	protected void redirectStreams() {
		final PrintStream ps = new PrintStream(this, true); // platform's default character set
		try {
			System.setOut(ps);
			System.setErr(ps);
			redirectLogging(); // Redirect java.util.logging
		}
		catch (final RuntimeException re) {
			log.log(Level.SEVERE, JFaceMessages.get("err.console.streams.redirect"), re);
		}
	}

	protected void redirectLogging() {
		final Logger rootLogger = LoggingSupport.getRootLogger();
		for (int i = 0; i < rootLogger.getHandlers().length; i++) {
			final Handler oldHandler = rootLogger.getHandlers()[i];
			if (oldHandler instanceof ConsoleHandler) {
				final ConsoleHandler newConsoleHandler = new ConsoleHandler();
				newConsoleHandler.setLevel(oldHandler.getLevel());
				newConsoleHandler.setFilter(oldHandler.getFilter());
				if (oldHandler.getFormatter() != null) {
					newConsoleHandler.setFormatter(oldHandler.getFormatter());
				}
				if (oldHandler.getErrorManager() != null) {
					newConsoleHandler.setErrorManager(oldHandler.getErrorManager());
				}
				try {
					newConsoleHandler.setEncoding(oldHandler.getEncoding());
				}
				catch (final UnsupportedEncodingException uee) {
					throw new IllegalStateException(uee);
				}
				rootLogger.removeHandler(oldHandler);
				rootLogger.addHandler(newConsoleHandler);
			}
		}
	}

	protected void resetStreams() {
		try {
			System.setOut(defaultSysOut);
			System.setErr(defaultSysErr);
		}
		catch (final RuntimeException re) {
			log.log(Level.SEVERE, JFaceMessages.get("err.console.streams.reset"), re);
		}
	}

	private void ensureOpen() throws IOException {
		if (byteBuffer == null) {
			throw new IOException("Stream closed");
		}
	}

	protected abstract int getDefaultLimit();

	public int getLimit() {
		try {
			return limit != null && limit.get() != null ? limit.get() : getDefaultLimit();
		}
		catch (final RuntimeException re) {
			log.log(Level.WARNING, JFaceMessages.get("err.console.capacity") + ' ' + JFaceMessages.get("err.configuration.using.default", getDefaultLimit()), re);
			return getDefaultLimit();
		}
	}

	public void setLimit(final ISupplier<Integer> limit) {
		this.limit = limit;
	}

	public void setLimit(final int limit) {
		this.limit = new Supplier<Integer>() {
			@Override
			public Integer get() {
				return limit;
			}
		};
	}

	public Font getFont() {
		return scrollable.getFont();
	}

	public void setFont(final Font font) {
		scrollable.setFont(font);
	}

	public void setFont(final FontData[] fds) {
		if (!Arrays.equals(fds, getFont().getFontData())) {
			final Font font = new Font(scrollable.getDisplay(), fds);
			scrollable.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(final DisposeEvent e) {
					font.dispose();
				}
			});
			setFont(font);
		}
	}

	public T getScrollable() {
		return scrollable;
	}

}
