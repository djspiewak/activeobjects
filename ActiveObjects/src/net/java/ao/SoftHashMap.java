/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.java.ao;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Implementation adapted from Apache Harmony
 */
class SoftHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
	private static final int DEFAULT_SIZE = 16;
	
	private Set<K> keySet;
	private Collection<V> valuesCollection;

	private final ReferenceQueue<K> referenceQueue;
	int elementCount;

	Entry<K, V>[] elementData;

	private final int loadFactor;
	private int threshold;
	volatile int modCount;

	// Simple utility method to isolate unchecked cast for array creation
	@SuppressWarnings("unchecked")
	private static <K, V> Entry<K, V>[] newEntryArray(int size) {
		return new Entry[size];
	}

	private static final class Entry<K, V> extends SoftReference<K> implements Map.Entry<K, V> {
		int hash;
		boolean isNull;
		V value;
		Entry<K, V> next;

		interface Type<R, K, V> {
			R get(Map.Entry<K, V> entry);
		}

		Entry(K key, V object, ReferenceQueue<K> queue) {
			super(key, queue);
			
			hash = key == null ? 0 : key.hashCode();
			value = object;
		}

		public K getKey() {
			return super.get();
		}

		public V getValue() {
			return value;
		}

		public V setValue(V object) {
			V result = value;
			value = object;
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Map.Entry)) {
				return false;
			}
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) other;
			Object key = super.get();
			return (key == null ? key == entry.getKey() : key.equals(entry.getKey())) && (value == null ? value == entry.getValue() : value.equals(entry.getValue()));
		}

		@Override
		public int hashCode() {
			return hash + (value == null ? 0 : value.hashCode());
		}

		@Override
		public String toString() {
			return super.get() + "=" + value; //$NON-NLS-1$
		}
	}

	class HashIterator<R> implements Iterator<R> {
		private int position = 0, expectedModCount;

		private Entry<K, V> currentEntry, nextEntry;

		private K nextKey;

		final Entry.Type<R, K, V> type;

		HashIterator(Entry.Type<R, K, V> type) {
			this.type = type;
			expectedModCount = modCount;
		}

		public boolean hasNext() {
			if (nextEntry != null && (nextKey != null || nextEntry.isNull)) {
				return true;
			}
			while (true) {
				if (nextEntry == null) {
					while (position < elementData.length) {
						if ((nextEntry = elementData[position++]) != null) {
							break;
						}
					}
					if (nextEntry == null) {
						return false;
					}
				}
				// ensure key of next entry is not gc'ed
				nextKey = nextEntry.get();
				if (nextKey != null || nextEntry.isNull) {
					return true;
				}
				nextEntry = nextEntry.next;
			}
		}

		public R next() {
			if (expectedModCount == modCount) {
				if (hasNext()) {
					currentEntry = nextEntry;
					nextEntry = currentEntry.next;
					R result = type.get(currentEntry);
					// free the key
					nextKey = null;
					return result;
				}
				throw new NoSuchElementException();
			}
			throw new ConcurrentModificationException();
		}

		public void remove() {
			if (expectedModCount == modCount) {
				if (currentEntry != null) {
					removeEntry(currentEntry);
					currentEntry = null;
					expectedModCount++;
					// cannot poll() as that would change the expectedModCount
				} else {
					throw new IllegalStateException();
				}
			} else {
				throw new ConcurrentModificationException();
			}
		}
	}

	public SoftHashMap() {
		this(DEFAULT_SIZE);
	}

	public SoftHashMap(int capacity) {
		if (capacity >= 0) {
			elementCount = 0;
			elementData = newEntryArray(capacity == 0 ? 1 : capacity);
			loadFactor = 7500; // Default load factor of 0.75
			computeMaxSize();
			referenceQueue = new ReferenceQueue<K>();
		} else {
			throw new IllegalArgumentException();
		}
	}

	public SoftHashMap(int capacity, float loadFactor) {
		if (capacity >= 0 && loadFactor > 0) {
			elementCount = 0;
			elementData = newEntryArray(capacity == 0 ? 1 : capacity);
			this.loadFactor = (int) (loadFactor * 10000);
			computeMaxSize();
			referenceQueue = new ReferenceQueue<K>();
		} else {
			throw new IllegalArgumentException();
		}
	}

	public SoftHashMap(Map<? extends K, ? extends V> map) {
		this(map.size() < 6 ? 11 : map.size() * 2);
		putAllImpl(map);
	}

	@Override
	public void clear() {
		if (elementCount > 0) {
			elementCount = 0;
			Arrays.fill(elementData, null);
			modCount++;
			while (referenceQueue.poll() != null) {
				// do nothing
			}
		}
	}

	private void computeMaxSize() {
		threshold = (int) ((long) elementData.length * loadFactor / 10000);
	}

	@Override
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		poll();
		return new AbstractSet<Map.Entry<K, V>>() {
			@Override
			public int size() {
				return SoftHashMap.this.size();
			}

			@Override
			public void clear() {
				SoftHashMap.this.clear();
			}

			@Override
			public boolean remove(Object object) {
				if (contains(object)) {
					SoftHashMap.this.remove(((Map.Entry<?, ?>) object).getKey());
					return true;
				}
				return false;
			}

			@Override
			public boolean contains(Object object) {
				if (object instanceof Map.Entry) {
					Entry<?, ?> entry = getEntry(((Map.Entry<?, ?>) object).getKey());
					if (entry != null) {
						Object key = entry.get();
						if (key != null || entry.isNull) {
							return object.equals(entry);
						}
					}
				}
				return false;
			}

			@Override
			public Iterator<Map.Entry<K, V>> iterator() {
				return new HashIterator<Map.Entry<K, V>>(new Entry.Type<Map.Entry<K, V>, K, V>() {
					public Map.Entry<K, V> get(Map.Entry<K, V> entry) {
						return entry;
					}
				});
			}
		};
	}

	@Override
	public Set<K> keySet() {
		poll();
		if (keySet == null) {
			keySet = new AbstractSet<K>() {
				@Override
				public boolean contains(Object object) {
					return containsKey(object);
				}

				@Override
				public int size() {
					return SoftHashMap.this.size();
				}

				@Override
				public void clear() {
					SoftHashMap.this.clear();
				}

				@Override
				public boolean remove(Object key) {
					if (containsKey(key)) {
						SoftHashMap.this.remove(key);
						return true;
					}
					return false;
				}

				@Override
				public Iterator<K> iterator() {
					return new HashIterator<K>(new Entry.Type<K, K, V>() {
						public K get(Map.Entry<K, V> entry) {
							return entry.getKey();
						}
					});
				}

				@Override
				public Object[] toArray() {
					Collection<K> coll = new ArrayList<K>(size());

					for (Iterator<K> iter = iterator(); iter.hasNext();) {
						coll.add(iter.next());
					}
					return coll.toArray();
				}

				@Override
				public <T> T[] toArray(T[] contents) {
					Collection<K> coll = new ArrayList<K>(size());

					for (Iterator<K> iter = iterator(); iter.hasNext();) {
						coll.add(iter.next());
					}
					return coll.toArray(contents);
				}
			};
		}
		return keySet;
	}

	@Override
	public Collection<V> values() {
		poll();
		if (valuesCollection == null) {
			valuesCollection = new AbstractCollection<V>() {
				@Override
				public int size() {
					return SoftHashMap.this.size();
				}

				@Override
				public void clear() {
					SoftHashMap.this.clear();
				}

				@Override
				public boolean contains(Object object) {
					return containsValue(object);
				}

				@Override
				public Iterator<V> iterator() {
					return new HashIterator<V>(new Entry.Type<V, K, V>() {
						public V get(Map.Entry<K, V> entry) {
							return entry.getValue();
						}
					});
				}
			};
		}
		return valuesCollection;
	}

	@Override
	public V get(Object key) {
		poll();
		if (key != null) {
			int index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
			Entry<K, V> entry = elementData[index];
			while (entry != null) {
				if (key.equals(entry.get())) {
					return entry.value;
				}
				entry = entry.next;
			}
			return null;
		}
		Entry<K, V> entry = elementData[0];
		while (entry != null) {
			if (entry.isNull) {
				return entry.value;
			}
			entry = entry.next;
		}
		return null;
	}

	Entry<K, V> getEntry(Object key) {
		poll();
		if (key != null) {
			int index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
			Entry<K, V> entry = elementData[index];
			while (entry != null) {
				if (key.equals(entry.get())) {
					return entry;
				}
				entry = entry.next;
			}
			return null;
		}
		Entry<K, V> entry = elementData[0];
		while (entry != null) {
			if (entry.isNull) {
				return entry;
			}
			entry = entry.next;
		}
		return null;
	}

	@Override
	public boolean containsValue(Object value) {
		poll();
		if (value != null) {
			for (int i = elementData.length; --i >= 0;) {
				Entry<K, V> entry = elementData[i];
				while (entry != null) {
					K key = entry.get();
					if ((key != null || entry.isNull) && value.equals(entry.value)) {
						return true;
					}
					entry = entry.next;
				}
			}
		} else {
			for (int i = elementData.length; --i >= 0;) {
				Entry<K, V> entry = elementData[i];
				while (entry != null) {
					K key = entry.get();
					if ((key != null || entry.isNull) && entry.value == null) {
						return true;
					}
					entry = entry.next;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@SuppressWarnings("unchecked")
	void poll() {
		Entry<K, V> toRemove;
		while ((toRemove = (Entry<K, V>) referenceQueue.poll()) != null) {
			removeEntry(toRemove);
		}
	}

	void removeEntry(Entry<K, V> toRemove) {
		Entry<K, V> entry, last = null;
		int index = (toRemove.hash & 0x7FFFFFFF) % elementData.length;
		entry = elementData[index];
		// Ignore queued entries which cannot be found, the user could
		// have removed them before they were queued, i.e. using clear()
		while (entry != null) {
			if (toRemove == entry) {
				modCount++;
				if (last == null) {
					elementData[index] = entry.next;
				} else {
					last.next = entry.next;
				}
				elementCount--;
				break;
			}
			last = entry;
			entry = entry.next;
		}
	}

	@Override
	public V put(K key, V value) {
		poll();
		int index = 0;
		Entry<K, V> entry;
		if (key != null) {
			index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
			entry = elementData[index];
			while (entry != null && !key.equals(entry.get())) {
				entry = entry.next;
			}
		} else {
			entry = elementData[0];
			while (entry != null && !entry.isNull) {
				entry = entry.next;
			}
		}
		if (entry == null) {
			modCount++;
			if (++elementCount > threshold) {
				rehash();
				index = key == null ? 0 : (key.hashCode() & 0x7FFFFFFF) % elementData.length;
			}
			entry = new Entry<K, V>(key, value, referenceQueue);
			entry.next = elementData[index];
			elementData[index] = entry;
			return null;
		}
		V result = entry.value;
		entry.value = value;
		return result;
	}

	private void rehash() {
		int length = elementData.length << 1;
		if (length == 0) {
			length = 1;
		}
		Entry<K, V>[] newData = newEntryArray(length);
		for (int i = 0; i < elementData.length; i++) {
			Entry<K, V> entry = elementData[i];
			while (entry != null) {
				int index = entry.isNull ? 0 : (entry.hash & 0x7FFFFFFF) % length;
				Entry<K, V> next = entry.next;
				entry.next = newData[index];
				newData[index] = entry;
				entry = next;
			}
		}
		elementData = newData;
		computeMaxSize();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		putAllImpl(map);
	}

	@Override
	public V remove(Object key) {
		poll();
		int index = 0;
		Entry<K, V> entry, last = null;
		if (key != null) {
			index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
			entry = elementData[index];
			while (entry != null && !key.equals(entry.get())) {
				last = entry;
				entry = entry.next;
			}
		} else {
			entry = elementData[0];
			while (entry != null && !entry.isNull) {
				last = entry;
				entry = entry.next;
			}
		}
		if (entry != null) {
			modCount++;
			if (last == null) {
				elementData[index] = entry.next;
			} else {
				last.next = entry.next;
			}
			elementCount--;
			return entry.value;
		}
		return null;
	}

	@Override
	public int size() {
		poll();
		return elementCount;
	}

	private void putAllImpl(Map<? extends K, ? extends V> map) {
		if (map.entrySet() != null) {
			super.putAll(map);
		}
	}
}