package it.albertus.jface.preference.field;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Text;

import it.albertus.jface.JFaceMessages;
import it.albertus.util.logging.LoggerFactory;

abstract class AbstractDateFieldEditor extends StringFieldEditor {

	private static final Logger log = LoggerFactory.getLogger(AbstractDateFieldEditor.class);

	protected final ThreadLocal<DateFormat> dateFormats = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			final DateFormat df = new SimpleDateFormat(pattern);
			df.setLenient(false);
			return df;
		}
	};

	private Composite parent;

	private String pattern;

	private int style;

	private DateTime dateTime;

	private Date minValidValue;
	private Date maxValidValue;

	private int validateStrategy;

	private ControlDecoration controlDecorator;

	/**
	 * Creates a date field editor of unlimited width with
	 * {@link StringFieldEditor#VALIDATE_ON_KEY_STROKE} strategy.
	 * 
	 * @see #AbstractDateFieldEditor(String, String, String, int, int, int,
	 *      Composite)
	 */
	protected AbstractDateFieldEditor(final String name, final String labelText, final String pattern, final int style, final Composite parent) {
		super(name, labelText, parent);
		init(pattern, style, VALIDATE_ON_KEY_STROKE, parent);
	}

	/**
	 * Creates a date field editor with
	 * {@link StringFieldEditor#VALIDATE_ON_KEY_STROKE} strategy.
	 * 
	 * @see #AbstractDateFieldEditor(String, String, String, int, int, int,
	 *      Composite)
	 */
	protected AbstractDateFieldEditor(final String name, final String labelText, final String pattern, final int style, final int width, final Composite parent) {
		super(name, labelText, width, parent);
		init(pattern, style, VALIDATE_ON_KEY_STROKE, parent);
	}

	/**
	 * Creates a date field editor.
	 * 
	 * @param name the name of the preference this field editor works on
	 * @param labelText the label text of the field editor
	 * @param pattern the date pattern expected
	 * @param style the style of the control, can be {@link SWT#NONE} or
	 *        {@link SWT#DROP_DOWN}
	 * @param width the width of the text input field in characters, or
	 *        <code>UNLIMITED</code> for no limit
	 * @param strategy the strategy for validating the text
	 * @param parent the parent of the field editor's control
	 */
	protected AbstractDateFieldEditor(final String name, final String labelText, final String pattern, final int style, final int width, final int strategy, final Composite parent) {
		super(name, labelText, width, strategy, parent);
		init(pattern, style, strategy, parent);
	}

	/** See {@link #doCreateControl}. */
	@Override
	protected void createControl(final Composite parent) {}

	@Override
	protected void valueChanged() {
		if (dateTime == null) {
			super.valueChanged();
		}
		else {
			setPresentsDefaultValue(false);
			final boolean oldState = isValid();
			refreshValidState();
			if (isValid() != oldState) {
				fireStateChanged(IS_VALID, oldState, isValid());
			}
			try {
				final String newValue = dateFormats.get().format(getDateValue());
				if (!newValue.equals(oldValue)) {
					fireValueChanged(VALUE, oldValue, newValue);
					oldValue = newValue;
				}
			}
			catch (final ParseException pe) {
				log.log(Level.SEVERE, JFaceMessages.get("err.date.parse"), pe);
			}
		}
	}

	@Override
	protected void doFillIntoGrid(final Composite parent, final int numColumns) {
		if (style == SWT.NONE) {
			super.doFillIntoGrid(parent, numColumns);
			addDecoration();
		}
		else {
			getLabelControl(parent);
			if (dateTime == null) {
				dateTime = getDateTimeControl(parent);
			}
			final GridData gd = new GridData();
			gd.horizontalSpan = numColumns - 1;
			gd.horizontalAlignment = GridData.FILL;
			gd.grabExcessHorizontalSpace = true;
			dateTime.setLayoutData(gd);
		}
	}

	@Override
	public Text getTextControl(final Composite parent) {
		if (style == SWT.NONE) {
			return super.getTextControl(parent);
		}
		throw new IllegalStateException("style is " + style);
	}

	public DateTime getDateTimeControl(final Composite parent) {
		if (dateTime != null) {
			return dateTime;
		}
		if (style == SWT.NONE) {
			throw new IllegalStateException("style is " + style);
		}
		final DateTime dt = new DateTime(parent, style);
		switch (validateStrategy) {
		case VALIDATE_ON_KEY_STROKE:
			dt.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					valueChanged();
				}
			});
			dt.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					valueChanged();
				}
			});
			break;
		case VALIDATE_ON_FOCUS_LOST:
			dt.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					clearErrorMessage();
				}
			});
			dt.addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					refreshValidState();
				}

				@Override
				public void focusLost(FocusEvent e) {
					valueChanged();
					clearErrorMessage();
				}
			});
			break;
		default:
			Assert.isTrue(false, "Unknown validate strategy");
		}
		return dt;
	}

	@Override
	protected void adjustForNumColumns(final int numColumns) {
		final GridData gd;
		if (style == SWT.NONE) {
			gd = (GridData) getTextControl().getLayoutData();
		}
		else {
			gd = (GridData) getDateTimeControl().getLayoutData();
		}
		if (gd != null) {
			gd.horizontalSpan = numColumns - 1;
			gd.grabExcessHorizontalSpace = gd.horizontalSpan == 1;
		}
		else {
			log.warning("layoutData is null");
		}
	}

	@Override
	protected boolean checkState() {
		if (dateTime == null) {
			return super.checkState();
		}
		else {
			boolean result = doCheckState();
			if (result) {
				clearErrorMessage();
			}
			else {
				showErrorMessage(getErrorMessage());
			}
			return result;
		}
	}

	@Override
	protected boolean doCheckState() {
		boolean result;
		Date date = null;
		if (isEmptyStringAllowed() && getTextControl() != null && getTextControl().getText().isEmpty()) {
			result = true;
		}
		else {
			try {
				date = getDateValue();
				result = true;
			}
			catch (final ParseException pe) {
				result = false;
			}
		}
		if (date != null && ((getMinValidValue() != null && date.before(getMinValidValue())) || (getMaxValidValue() != null && date.after(getMaxValidValue())))) {
			result = false;
		}
		return result;
	}

	@Override
	protected void doLoad() {
		if (dateTime == null) {
			final Text textField = getTextControl();
			if (textField != null) {
				String value = getPreferenceStore().getString(getPreferenceName());
				try { // Format
					final DateFormat df = dateFormats.get();
					value = df.format(df.parse(value));
				}
				catch (final ParseException pe) {/* Ignore */}
				textField.setText(value);
				oldValue = value;
			}
		}
		else {
			final String value = getPreferenceStore().getString(getPreferenceName());
			final Calendar calendar = Calendar.getInstance();
			try {
				calendar.setTime(dateFormats.get().parse(value));
			}
			catch (final ParseException pe) {/* Ignore */}
			setDateTimeValue(calendar);
			oldValue = value;
		}
	}

	@Override
	protected void doLoadDefault() {
		if (dateTime == null) {
			super.doLoadDefault();
			getTextControl().notifyListeners(SWT.KeyUp, null);
		}
		else {
			final String value = getPreferenceStore().getDefaultString(getPreferenceName());
			final Calendar calendar = Calendar.getInstance();
			try {
				calendar.setTime(dateFormats.get().parse(value));
			}
			catch (final ParseException pe) {/* Ignore */}
			setDateTimeValue(calendar);
			valueChanged();
		}
	}

	@Override
	protected void doStore() {
		if (dateTime == null) {
			super.doStore();
		}
		else {
			try {
				final Date date = getDateValue();
				final String dateString = dateFormats.get().format(date);
				getPreferenceStore().setValue(getPreferenceName(), dateString);
			}
			catch (final ParseException pe) {
				log.log(Level.SEVERE, JFaceMessages.get("err.date.parse"), pe);
			}
		}
	}

	protected void setDateTimeValue(final Calendar calendar) {
		dateTime.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		dateTime.setTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
	}

	protected void setDateTimeValue(final Date date) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setLenient(false);
		calendar.setTime(date);
		setDateTimeValue(calendar);
	}

	public void setValidRange(final Date from, final Date to) {
		setMinValidValue(from);
		setMaxValidValue(to);
	}

	protected void checkPattern(final String pattern) {
		if (pattern == null) {
			throw new NullPointerException("pattern cannot be null");
		}
	}

	protected void init(final String pattern, final int style, final int validateStrategy, final Composite parent) {
		checkPattern(pattern);
		this.pattern = pattern;
		this.parent = parent;
		this.style = style;
		this.validateStrategy = validateStrategy;
		updateErrorMessage();
		setTextLimit(Byte.MAX_VALUE);
		doCreateControl();
	}

	protected void doCreateControl() {
		final GridLayout layout = new GridLayout();
		layout.numColumns = getNumberOfControls();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = HORIZONTAL_GAP;
		parent.setLayout(layout);
		doFillIntoGrid(parent, layout.numColumns);
		getLabelControl().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				dateFormats.remove();
			}
		});
	}

	protected void addDecoration() {
		controlDecorator = new ControlDecoration(getTextControl(), SWT.TOP | SWT.LEFT);
		controlDecorator.hide();
		final Image image = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();
		controlDecorator.setImage(image);
	}

	@Override
	protected void showErrorMessage(final String msg) {
		super.showErrorMessage(msg);
		if (controlDecorator != null) {
			controlDecorator.setDescriptionText(msg);
			controlDecorator.show();
		}
	}

	@Override
	protected void clearErrorMessage() {
		super.clearErrorMessage();
		if (controlDecorator != null) {
			controlDecorator.hide();
		}
	}

	public DateTime getDateTimeControl() {
		return dateTime;
	}

	public String getPattern() {
		return pattern;
	}

	public int getStyle() {
		return style;
	}

	public Date getDateValue() throws ParseException {
		if (dateTime == null) {
			return dateFormats.get().parse(getTextControl().getText());
		}
		else {
			final Calendar calendar = Calendar.getInstance();
			calendar.setLenient(false);
			calendar.set(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHours(), dateTime.getMinutes(), dateTime.getSeconds());
			calendar.set(Calendar.MILLISECOND, 0);
			return calendar.getTime();
		}
	}

	public Date getMinValidValue() {
		return minValidValue;
	}

	public void setMinValidValue(final Date minValidValue) {
		this.minValidValue = minValidValue;
		updateErrorMessage();
	}

	public Date getMaxValidValue() {
		return maxValidValue;
	}

	public void setMaxValidValue(final Date maxValidValue) {
		this.maxValidValue = maxValidValue;
		updateErrorMessage();
	}

	public int getValidateStrategy() {
		return validateStrategy;
	}

	@Override
	public void setValidateStrategy(final int value) {
		super.setValidateStrategy(value);
		this.validateStrategy = value;
	}

	protected void updateErrorMessage() {
		if (getMinValidValue() == null && getMaxValidValue() == null) {
			setErrorMessage(JFaceMessages.get("err.preferences.date", pattern));
		}
		else {
			final DateFormat df = dateFormats.get();
			if (getMinValidValue() != null && getMaxValidValue() == null) {
				setErrorMessage(JFaceMessages.get("err.preferences.date.from", df.format(getMinValidValue())));
			}
			else if (getMinValidValue() == null && getMaxValidValue() != null) {
				setErrorMessage(JFaceMessages.get("err.preferences.date.to", df.format(getMaxValidValue())));
			}
			else {
				setErrorMessage(JFaceMessages.get("err.preferences.date.range", df.format(getMinValidValue()), df.format(getMaxValidValue())));
			}
		}
	}

	@Override
	public void setEnabled(final boolean enabled, final Composite parent) {
		if (style == SWT.NONE) {
			super.setEnabled(enabled, parent);
		}
		else {
			getLabelControl(parent).setEnabled(enabled);
			getDateTimeControl(parent).setEnabled(enabled);
		}
	}

}
