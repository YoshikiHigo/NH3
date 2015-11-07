package yoshikihigo.fbparser;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

public class LocationTransition {

	final private SortedMap<Integer, Location> locations;

	public LocationTransition() {
		this.locations = new TreeMap<>();
	}

	public void add(final Integer revision, final Location location) {
		this.locations.put(revision, location);
	}

	public Location getLocation(final Integer revision) {

		Location location = null;
		for (final Entry<Integer, Location> entry : this.locations.entrySet()) {

			final Integer currentRevision = entry.getKey();
			final Location currentLocation = entry.getValue();

			if (revision < currentRevision) {
				return location;
			}

			else {
				location = currentLocation;
			}
		}

		return location;
	}

	public Location getInitialLocation() {
		final Integer firstRevision = this.locations.firstKey();
		final Location location = this.locations.get(firstRevision);
		return location;
	}

	public Location getLatestLocation() {
		final Integer lastRevision = this.locations.lastKey();
		final Location range = this.locations.get(lastRevision);
		return range;
	}

	public boolean hasChanged() {
		final Location latestRange = this.getLatestLocation();
		if (latestRange instanceof Location_ADDITION
				|| latestRange instanceof Location_DELETION
				|| latestRange instanceof Location_REPLACEMENT
				|| latestRange instanceof Location_UNKNOWN) {
			return true;
		} else {
			return false;
		}
	}

	public Integer[] getChangedRevisions() {
		return this.locations.keySet().toArray(new Integer[0]);
	}
}
