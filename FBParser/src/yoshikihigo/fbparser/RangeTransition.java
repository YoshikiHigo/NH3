package yoshikihigo.fbparser;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

public class RangeTransition {

	final private SortedMap<Long, Range> transition;

	public RangeTransition() {
		this.transition = new TreeMap<>();
	}

	public void add(final Long revision, final Range range) {
		this.transition.put(revision, range);
	}

	public Range getRange(final Long revision) {

		final Long firstRevision = this.transition.firstKey();
		if (revision < firstRevision) {
			return null;
		}

		Range range = null;
		for (final Entry<Long, Range> entry : this.transition.entrySet()) {

			final Long currentRevision = entry.getKey();
			final Range currentRange = entry.getValue();

			if (revision < currentRevision) {
				return range;
			}

			else {
				range = currentRange;
			}
		}

		return range;
	}

	public Range getLatestRange() {
		final Long lastRevision = this.transition.lastKey();
		final Range range = this.transition.get(lastRevision);
		return range;
	}

	public boolean hasDisappeared() {
		return null == this.getLatestRange();
	}

	public Long[] getChangedRevisions() {
		return this.transition.keySet().toArray(new Long[0]);
	}
}
