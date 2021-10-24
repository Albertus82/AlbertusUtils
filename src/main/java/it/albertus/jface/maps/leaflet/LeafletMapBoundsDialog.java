package it.albertus.jface.maps.leaflet;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import it.albertus.jface.DisplayThreadExecutor;
import it.albertus.jface.DisplayThreadExecutor.Mode;
import it.albertus.jface.JFaceMessages;
import it.albertus.jface.maps.CoordinateUtils;
import it.albertus.jface.maps.MapBounds;
import it.albertus.jface.maps.MapBoundsDialog;
import it.albertus.jface.maps.MapBoundsDialogCreationHelper;
import it.albertus.util.logging.LoggerFactory;

public class LeafletMapBoundsDialog extends LeafletMapDialog implements MapBoundsDialog {

	private static final String MAP_ONEVENTS_FN = "mapOnEvents";

	private static final Logger log = LoggerFactory.getLogger(LeafletMapBoundsDialog.class);

	private MapBounds bounds = new MapBounds();

	private final MapBoundsDialogCreationHelper helper = new MapBoundsDialogCreationHelper(this);

	public LeafletMapBoundsDialog(final Shell parent) {
		super(parent);
	}

	public LeafletMapBoundsDialog(final Shell parent, final int style) {
		super(parent, style);
	}

	@Override
	protected Layout getLayout() {
		return helper.getLayout();
	}

	@Override
	protected void createContents(final Shell shell) {
		shell.setLayout(getLayout());

		final Composite c = new Composite(shell, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(c);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(c);

		final Group latitudeGroup = new Group(c, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(latitudeGroup);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(latitudeGroup);
		latitudeGroup.setText(JFaceMessages.get("lbl.map.bounds.range.latitude"));

		final Group longitudeGroup = new Group(c, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(longitudeGroup);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(longitudeGroup);
		longitudeGroup.setText(JFaceMessages.get("lbl.map.bounds.range.longitude"));

		final Label southWestLatLabel = new Label(latitudeGroup, SWT.NONE);
		GridDataFactory.swtDefaults().applyTo(southWestLatLabel);
		southWestLatLabel.setText(JFaceMessages.get("lbl.map.bounds.range.from"));
		final Text southWestLatText = new Text(latitudeGroup, SWT.BORDER | SWT.READ_ONLY);
		southWestLatText.setEditable(false);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(southWestLatText);

		final Label northEastLatLabel = new Label(latitudeGroup, SWT.NONE);
		GridDataFactory.swtDefaults().applyTo(northEastLatLabel);
		northEastLatLabel.setText(JFaceMessages.get("lbl.map.bounds.range.to"));
		final Text northEastLatText = new Text(latitudeGroup, SWT.BORDER | SWT.READ_ONLY);
		northEastLatText.setEditable(false);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(northEastLatText);

		final Label southWestLngLabel = new Label(longitudeGroup, SWT.NONE);
		GridDataFactory.swtDefaults().applyTo(southWestLngLabel);
		southWestLngLabel.setText(JFaceMessages.get("lbl.map.bounds.range.from"));
		final Text southWestLngText = new Text(longitudeGroup, SWT.BORDER | SWT.READ_ONLY);
		southWestLngText.setEditable(false);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(southWestLngText);

		final Label northEastLngLabel = new Label(longitudeGroup, SWT.NONE);
		GridDataFactory.swtDefaults().applyTo(northEastLngLabel);
		northEastLngLabel.setText(JFaceMessages.get("lbl.map.bounds.range.to"));
		final Text northEastLngText = new Text(longitudeGroup, SWT.BORDER | SWT.READ_ONLY);
		northEastLngText.setEditable(false);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(northEastLngText);

		final Browser browser = createBrowser(shell);

		final BrowserFunction function = new BrowserFunction(browser, MAP_ONEVENTS_FN) {
			private final NumberFormat formatter = CoordinateUtils.newFormatter();

			@Override
			public Object function(final Object[] arguments) {
				final MapBounds mb = MapBounds.normalize(getBoundValues(browser));
				if (mb.getSouthWestLat() != null && mb.getNorthEastLat() != null) {
					southWestLatText.setText(formatter.format(mb.getSouthWestLat()));
					northEastLatText.setText(formatter.format(mb.getNorthEastLat()));
				}
				if (mb.getSouthWestLng() != null && mb.getNorthEastLng() != null) {
					southWestLngText.setText(formatter.format(mb.getSouthWestLng()));
					northEastLngText.setText(formatter.format(mb.getNorthEastLng()));
				}
				return null;
			}
		};
		browser.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				function.dispose();
			}
		});

		createButtonBox(browser);

		browser.setFocus();

		final Thread updateBoundsThread = new Thread() { // useful if the Browser is not fully functional
			@Override
			public void run() {
				while (!browser.isDisposed()) {
					try {
						TimeUnit.SECONDS.sleep(1);
					}
					catch (final InterruptedException e) {
						Thread.currentThread().interrupt();
						log.log(Level.FINE, "Interrupted:", e);
					}
					new DisplayThreadExecutor(browser, Mode.SYNC).execute(new Runnable() {
						@Override
						public void run() {
							if (!browser.isDisposed()) {
								log.log(Level.FINE, "Updating bound fields from map...");
								try {
									function.function(null);
									log.log(Level.FINE, "Bound fields updated successfully.");
								}
								catch (final RuntimeException e) {
									log.log(Level.FINE, "Cannot update bound fields from map:", e);
								}
							}
						}
					});
				}
				log.log(Level.FINE, "Browser disposed -> Terminating {0}", this);
			}
		};
		updateBoundsThread.setDaemon(true);
		updateBoundsThread.setPriority(Thread.MIN_PRIORITY);
		updateBoundsThread.start();
	}

	@Override
	protected Browser createBrowser(final Composite parent) {
		return helper.createBrowser(parent, getMapPage(parent));
	}

	@Override
	public Composite createButtonBox(final Browser browser) {
		return helper.createButtonBox(browser);
	}

	@Override
	public MapBounds getBounds() {
		return bounds;
	}

	@Override
	public String parseLine(final String line) {
		return parseLine(line, getOptions(), getMarkers(), String.format("map.on('move', function(e) { %s(); }); document.onload = %s();", MAP_ONEVENTS_FN, MAP_ONEVENTS_FN));
	}

	@Override
	public void setOptionValues(final Browser browser) {
		getOptions().setZoom(((Number) browser.evaluate("return map.getZoom();")).intValue());
		getOptions().setCenterLat((Double) browser.evaluate("return map.getCenter().lat;"));
		getOptions().setCenterLng((Double) browser.evaluate("return map.getCenter().lng;"));
	}

	@Override
	public void setBoundValues(final Browser browser) {
		bounds = MapBounds.normalize(getBoundValues(browser));
	}

	private static MapBounds getBoundValues(final Browser browser) {
		final Double northEastLat = (Double) browser.evaluate("return map.getBounds().getNorthEast().lat;");
		final Double southWestLat = (Double) browser.evaluate("return map.getBounds().getSouthWest().lat;");
		final Double northEastLng = (Double) browser.evaluate("return map.getBounds().getNorthEast().lng;");
		final Double southWestLng = (Double) browser.evaluate("return map.getBounds().getSouthWest().lng;");
		return new MapBounds(northEastLat, southWestLat, northEastLng, southWestLng);
	}

}
