package it.albertus.jface;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.eclipse.swt.widgets.Event;

import it.albertus.util.IOUtils;
import it.albertus.util.logging.LoggerFactory;

/**
 * Utility class for SWT events.
 * 
 * @see org.eclipse.swt.widgets.Event
 */
public class Events {

	private static final String EVENT_NAMES_RESOURCE_NAME = "event-names.properties";

	private static final Properties eventNames;

	static {
		eventNames = new Properties();
		InputStream is = null;
		try {
			is = Events.class.getResourceAsStream(EVENT_NAMES_RESOURCE_NAME);
			eventNames.load(is);
		}
		catch (final IOException e) {
			LoggerFactory.getLogger(Events.class).log(Level.WARNING, "Unable to load resource " + EVENT_NAMES_RESOURCE_NAME, e);
		}
		finally {
			IOUtils.closeQuietly(is);
		}
	}

	private Events() {
		throw new IllegalAccessError("Utility class");
	}

	/**
	 * Returns the event name corresponding to the provided type, as defined in
	 * {@link org.eclipse.swt.SWT SWT} class.
	 * 
	 * @param type the event type
	 * @return the event name, or null if there's no match for the provided type
	 * @see #getName(Event)
	 * @see org.eclipse.swt.SWT
	 */
	@Nullable
	public static String getName(final int type) {
		return eventNames.getProperty(Integer.toString(type));
	}

	/**
	 * Returns the event name corresponding to the provided event object, as defined
	 * in {@link org.eclipse.swt.SWT SWT} class.
	 * 
	 * @param event the event object
	 * @return the event name, or null if the argument is null or there's no match
	 *         for the event's type
	 * @see #getName(int)
	 * @see org.eclipse.swt.SWT
	 */
	@Nullable
	public static String getName(@Nullable final Event event) {
		return event != null ? getName(event.type) : null;
	}

}
