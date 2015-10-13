package yoshikihigo.fbparser;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

public class WarningLocationTransition {

	final private SortedMap<Long, Location> locations;

	public WarningLocationTransition() {
		this.locations = new TreeMap<>();
	}

	public void add(final Long revision, final Location range) {
		this.locations.put(revision, range);
	}

	public Location getRange(final Long revision) {

		Location range = null;
		for (final Entry<Long, Location> entry : this.locations.entrySet()) {

			final Long currentRevision = entry.getKey();
			final Location currentRange = entry.getValue();

			if (revision < currentRevision) {
				return range;
			}

			else {
				range = currentRange;
			}
		}

		return range;
	}

	public Location getFirstRange() {
		final Long firstRevision = this.locations.firstKey();
		final Location range = this.locations.get(firstRevision);
		return range;
	}

	public Location getLastRange() {
		final Long lastRevision = this.locations.lastKey();
		final Location range = this.locations.get(lastRevision);
		return range;
	}

	public boolean hasDisappeared() {
		final Location latestRange = this.getLastRange();
		if (latestRange instanceof Location_ADDITION
				|| latestRange instanceof Location_DELETION
				|| latestRange instanceof Location_REPLACEMENT
				|| latestRange instanceof Location_UNKNOWN) {
			return true;
		} else {
			return false;
		}
	}

	public Long[] getChangedRevisions() {
		return this.locations.keySet().toArray(new Long[0]);
	}
}
