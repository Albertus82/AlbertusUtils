package it.albertus.jface.maps.leaflet;

import java.util.Collection;
import java.util.Map.Entry;

import org.eclipse.swt.widgets.Shell;

import it.albertus.jface.maps.MapDialog;
import it.albertus.jface.maps.MapMarker;
import it.albertus.net.httpserver.html.HtmlUtils;
import it.albertus.util.NewLine;

public class LeafletMapDialog extends MapDialog {

	private final LeafletMapOptions options = new LeafletMapOptions();

	public LeafletMapDialog(final Shell parent) {
		super(parent);
	}

	public LeafletMapDialog(final Shell parent, final int style) {
		super(parent, style);
	}

	public static String parseLine(final String line, final LeafletMapOptions options, final Collection<MapMarker> markers, final String other) {
		// Options
		if (line.contains(OPTIONS_PLACEHOLDER)) {
			final StringBuilder optionsBlock = new StringBuilder();
			optionsBlock.append(String.format("map.setView([%s, %s], %d);", options.getCenterLat(), options.getCenterLng(), options.getZoom()));
			if (!options.getControls().containsKey(LeafletMapControl.LAYERS)) {
				optionsBlock.append(NewLine.SYSTEM_LINE_SEPARATOR);
				optionsBlock.append(String.format("L.tileLayer('%s', { maxZoom: %d, attribution: '%s' }).addTo(map);", HtmlUtils.escapeEcmaScript("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"), 19, HtmlUtils.escapeEcmaScript("&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a>")));
			}
			for (final Entry<LeafletMapControl, String> control : options.getControls().entrySet()) {
				optionsBlock.append(NewLine.SYSTEM_LINE_SEPARATOR);
				optionsBlock.append(String.format("map.addControl(L.control.%s(%s));", control.getKey().getConstructor(), control.getValue() == null ? "" : control.getValue().trim()));
			}
			return optionsBlock.toString().trim();
		}
		// Markers
		else if (line.contains(MARKERS_PLACEHOLDER)) {
			if (markers == null || markers.isEmpty()) {
				return null;
			}
			else {
				final StringBuilder markersBlock = new StringBuilder();
				for (final MapMarker marker : markers) {
					markersBlock.append(String.format("L.marker([%s, %s]).addTo(map).bindPopup('%s');", marker.getLatitude(), marker.getLongitude(), marker.getTitle() == null ? "" : HtmlUtils.escapeEcmaScript(marker.getTitle().replace(NewLine.SYSTEM_LINE_SEPARATOR, "<br />").trim())));
					markersBlock.append(NewLine.SYSTEM_LINE_SEPARATOR);
				}
				return markersBlock.toString().trim();
			}
		}
		else if (line.contains(OTHER_PLACEHOLDER)) {
			if (other == null || other.isEmpty()) {
				return null;
			}
			else {
				return other;
			}
		}
		else {
			return line;
		}
	}

	@Override
	public String parseLine(final String line) {
		return parseLine(line, getOptions(), getMarkers(), null);
	}

	@Override
	public LeafletMapOptions getOptions() {
		return options;
	}

}
