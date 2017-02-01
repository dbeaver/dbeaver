/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.utils;

import java.util.*;

/**
	Map with long key.
*/
public class LongKeyMap<VALUE> implements Map<Long, VALUE> {
	/**
	 * The default initial capacity - MUST be a power of two.
	 */
	static final int DEFAULT_INITIAL_CAPACITY = 16;

	/**
	 * The maximum capacity, used if a higher value is implicitly specified
	 * by either of the constructors with arguments.
	 * MUST be a power of two <= 1<<30.
	 */
	static final int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The load fast used when none specified in constructor.
	 **/
	static final float DEFAULT_LOAD_FACTOR = 0.75f;

	/**
	 * The table, resized as necessary. Length MUST Always be a power of two.
	 */
	transient LongEntry<VALUE>[] table;

	/**
	 * The number of key-value mappings contained in this identity hash map.
	 */
	transient int size;

	/**
	 * The next size value at which to resize (capacity * load factor).
	 * @serial
	 */
	int threshold;

	/**
	 * The load factor for the hash table.
	 *
	 * @serial
	 */
	final float loadFactor;

	/**
	 * The number of times this LongKeyMap has been structurally modified
	 */
	transient volatile int modCount;

	/**
	 * Constructs an empty <tt>LongKeyMap</tt> with the specified initial
	 * capacity and load factor.
	 *
	 * @param  initialCapacity The initial capacity.
	 * @param  loadFactor      The load factor.
	 * @throws IllegalArgumentException if the initial capacity is negative
	 *         or the load factor is nonpositive.
	 */
	public LongKeyMap(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal initial capacity: " +
											   initialCapacity);
		if (initialCapacity > MAXIMUM_CAPACITY)
			initialCapacity = MAXIMUM_CAPACITY;
		if (loadFactor <= 0 || Float.isNaN(loadFactor))
			throw new IllegalArgumentException("Illegal load factor: " +
											   loadFactor);

		// Find a power of 2 >= initialCapacity
		int capacity = 1;
		while (capacity < initialCapacity)
			capacity <<= 1;

		this.loadFactor = loadFactor;
		threshold = (int)(capacity * loadFactor);
		table = new LongEntry[capacity];
	}

	/**
	 * Constructs an empty <tt>LongKeyMap</tt> with the specified initial
	 * capacity and the default load factor (0.75).
	 *
	 * @param  initialCapacity the initial capacity.
	 * @throws IllegalArgumentException if the initial capacity is negative.
	 */
	public LongKeyMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs an empty <tt>LongKeyMap</tt> with the default initial capacity
	 * (16) and the default load factor (0.75).
	 */
	public LongKeyMap() {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
		threshold = DEFAULT_INITIAL_CAPACITY;
		table = new LongEntry[DEFAULT_INITIAL_CAPACITY];
	}

	static int hash(long x) {
		int h = (int)(x ^ (x >>> 32));
		h += ~(h << 9);
		h ^=  (h >>> 14);
		h +=  (h << 4);
		h ^=  (h >>> 10);
		return h;
	}

	/**
	 * Returns index for hash code h.
	 */
	static int indexFor(int h, int length) {
		return h & (length-1);
	}

	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map.
	 */
	@Override
    public int size() {
		return size;
	}

	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 *
	 * @return <tt>true</tt> if this map contains no key-value mappings.
	 */
	@Override
    public boolean isEmpty()
	{
		return size == 0;
	}

	@Override
    public boolean containsKey(Object key)
	{
		return containsKey(((Number)key).longValue());
	}

	/**
	 * Returns the value to which the specified key is mapped in this identity
	 * hash map, or <tt>null</tt> if the map contains no mapping for this key.
	 * A return value of <tt>null</tt> does not <i>necessarily</i> indicate
	 * that the map contains no mapping for the key; it is also possible that
	 * the map explicitly maps the key to <tt>null</tt>. The
	 * <tt>containsKey</tt> method may be used to distinguish these two cases.
	 *
	 * @param   key the key whose associated value is to be returned.
	 * @return  the value to which this map maps the specified key, or
	 *          <tt>null</tt> if the map contains no mapping for this key.
	 * @see #put(long, Object)
	 */
	public VALUE get(long key) {
		int hash = hash(key);
		int i = indexFor(hash, table.length);
		LongEntry<VALUE> e = table[i];
		while (true) {
			if (e == null)
				return null;
			if (e.hash == hash && key == e.key)
				return e.value;
			e = e.next;
		}
	}

	/**
	 * Returns <tt>true</tt> if this map contains a mapping for the
	 * specified key.
	 */
	public boolean containsKey(long key)
	{
		int hash = hash(key);
		int i = indexFor(hash, table.length);
		LongEntry<VALUE> e = table[i];
		while (e != null) {
			if (e.hash == hash && key == e.key)
				return true;
			e = e.next;
		}
		return false;
	}

	/**
	 * Returns the entry associated with the specified key in the
	 * LongKeyMap.  Returns null if the LongKeyMap contains no mapping
	 * for this key.
	 */
	LongEntry<VALUE> getEntry(long key) {
		int hash = hash(key);
		int i = indexFor(hash, table.length);
		LongEntry<VALUE> e = table[i];
		while (e != null && !(e.hash == hash && key == e.key))
			e = e.next;
		return e;
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for this key, the old
	 * value is replaced.
	 *
	 * @param key key with which the specified value is to be associated.
	 * @param value value to be associated with the specified key.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *	       if there was no mapping for key.  A <tt>null</tt> return can
	 *	       also indicate that the LongKeyMap previously associated
	 *	       <tt>null</tt> with the specified key.
	 */
	public VALUE put(long key, VALUE value) {
		int hash = hash(key);
		int i = indexFor(hash, table.length);

		for (LongEntry<VALUE> e = table[i]; e != null; e = e.next) {
			if (e.hash == hash && key == e.key) {
				VALUE oldValue = e.value;
				e.value = value;
				return oldValue;
			}
		}

		modCount++;
		addEntry(hash, key, value, i);
		return null;
	}

	/**
	 * This method is used instead of put by constructors and
	 * pseudoconstructors (clone, readObject).  It does not resize the table,
	 * check for comodification, etc.  It calls createEntry rather than
	 * addEntry.
	 */
	private void putForCreate(long key, VALUE value) {
		int hash = hash(key);
		int i = indexFor(hash, table.length);

		/**
		 * Look for preexisting entry for key.  This will never happen for
		 * clone or deserialize.  It will only happen for construction if the
		 * input Map is a sorted map whose ordering is inconsistent w/ equals.
		 */
		for (LongEntry<VALUE> e = table[i]; e != null; e = e.next) {
			if (e.hash == hash && key == e.key) {
				e.value = value;
				return;
			}
		}

		createEntry(hash, key, value, i);
	}

	void putAllForCreate(LongKeyMap<VALUE> m) {
		for (Iterator<LongEntry<VALUE>> i = m.entrySet().iterator(); i.hasNext(); ) {
			LongEntry<VALUE> e = i.next();
			putForCreate(e.key, e.value);
		}
	}

	/**
	 * Rehashes the contents of this map into a new <tt>LongKeyMap</tt> instance
	 * with a larger capacity. This method is called automatically when the
	 * number of keys in this map exceeds its capacity and load factor.
	 *
	 * @param newCapacity the new capacity, MUST be a power of two.
	 */
	void resize(int newCapacity) {
		// assert (newCapacity & -newCapacity) == newCapacity; // power of 2
		LongEntry<VALUE>[] oldTable = table;
		int oldCapacity = oldTable.length;

		// check if needed
		if (size < threshold || oldCapacity > newCapacity)
			return;

		LongEntry[] newTable = new LongEntry[newCapacity];
		transfer(newTable);
		table = newTable;
		threshold = (int)(newCapacity * loadFactor);
	}

	/**
	 * Transfer all entries from current table to newTable.
	 */
	void transfer(LongEntry[] newTable) {
		LongEntry<VALUE>[] src = table;
		int newCapacity = newTable.length;
		for (int j = 0; j < src.length; j++) {
			LongEntry<VALUE> e = src[j];
			if (e != null) {
				src[j] = null;
				do {
					LongEntry<VALUE> next = e.next;
					int i = indexFor(e.hash, newCapacity);
					e.next = newTable[i];
					newTable[i] = e;
					e = next;
				} while (e != null);
			}
		}
	}

	/**
	 * Copies all of the mappings from the specified map to this map
	 * These mappings will replace any mappings that
	 * this map had for any of the keys currently in the specified map.
	 *
	 * @param t mappings to be stored in this map.
	 * @throws NullPointerException if the specified map is null.
	 */
	public void putAll(LongKeyMap<VALUE> t) {
		// Expand enough to hold t's elements without resizing.
		int n = t.size();
		if (n == 0)
			return;
		if (n >= threshold) {
			n = (int)(n / loadFactor + 1);
			if (n > MAXIMUM_CAPACITY)
				n = MAXIMUM_CAPACITY;
			int capacity = table.length;
			while (capacity < n)
				capacity <<= 1;
			resize(capacity);
		}

		for (Iterator<LongEntry<VALUE>> i = t.entrySet().iterator(); i.hasNext(); ) {
			LongEntry<VALUE> e = i.next();
			put(e.key, e.value);
		}
	}

	/**
	 * Removes the mapping for this key from this map if present.
	 *
	 * @param  key key whose mapping is to be removed from the map.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *	       if there was no mapping for key.  A <tt>null</tt> return can
	 *	       also indicate that the map previously associated <tt>null</tt>
	 *	       with the specified key.
	 */
	public VALUE remove(long key) {
		LongEntry<VALUE> e = removeEntryForKey(key);
		return (e == null ? null : e.value);
	}

	/**
	 * Removes and returns the entry associated with the specified key
	 * in the LongKeyMap.  Returns null if the LongKeyMap contains no mapping
	 * for this key.
	 */
	LongEntry<VALUE> removeEntryForKey(long key) {
		int hash = hash(key);
		int i = indexFor(hash, table.length);
		LongEntry<VALUE> prev = table[i];
		LongEntry<VALUE> e = prev;

		while (e != null) {
			LongEntry<VALUE> next = e.next;
			if (e.hash == hash && key == e.key) {
				modCount++;
				size--;
				if (prev == e)
					table[i] = next;
				else
					prev.next = next;
				return e;
			}
			prev = e;
			e = next;
		}

		return e;
	}

	/**
	 * Special version of remove for EntrySet.
	 */
	LongEntry<VALUE> removeMapping(Object o) {
		if (!(o instanceof LongEntry))
			return null;

		LongEntry<VALUE> entry = (LongEntry<VALUE>)o;
		int hash = hash(entry.key);
		int i = indexFor(hash, table.length);
		LongEntry<VALUE> prev = table[i];
		LongEntry<VALUE> e = prev;

		while (e != null) {
			LongEntry<VALUE> next = e.next;
			if (e.hash == hash && e.equals(entry)) {
				modCount++;
				size--;
				if (prev == e)
					table[i] = next;
				else
					prev.next = next;
				return e;
			}
			prev = e;
			e = next;
		}

		return e;
	}

	/**
	 * Removes all mappings from this map.
	 */
	@Override
    public void clear() {
		modCount++;
		LongEntry<VALUE> tab[] = table;
		for (int i = 0; i < tab.length; i++)
			tab[i] = null;
		size = 0;
	}

	/**
	 * Returns <tt>true</tt> if this map maps one or more keys to the
	 * specified value.
	 *
	 * @param value value whose presence in this map is to be tested.
	 * @return <tt>true</tt> if this map maps one or more keys to the
	 *         specified value.
	 */
	@Override
    public boolean containsValue(Object value)
	{
		if (value == null)
			return containsNullValue();

		LongEntry<VALUE> tab[] = table;
		for (int i = 0; i < tab.length ; i++)
			for (LongEntry<VALUE> e = tab[i] ; e != null ; e = e.next)
				if (value.equals(e.value))
					return true;
		return false;
	}

	@Override
    public VALUE get(Object key)
	{
		return get(((Number)key).longValue());
	}

	@Override
    public VALUE put(Long key, VALUE value)
	{
		return put(key.longValue(), value);
	}

	@Override
    public VALUE remove(Object key)
	{
		return remove(((Number)key).longValue());
	}

	@Override
    public void putAll(Map<? extends Long, ? extends VALUE> t)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Special-case code for containsValue with null argument
	 **/
	private boolean containsNullValue()
	{
		LongEntry<VALUE> tab[] = table;
		for (int i = 0; i < tab.length ; i++)
			for (LongEntry<VALUE> e = tab[i] ; e != null ; e = e.next)
				if (e.value == null)
					return true;
		return false;
	}

	public static class LongEntry<VALUE> implements Entry<Long, VALUE> {
		final long key;
		VALUE value;
		final int hash;
		LongEntry<VALUE> next;

		/**
		 * Create new entry.
		 */
		LongEntry(int h, long k, VALUE v, LongEntry<VALUE> n) {
			value = v;
			next = n;
			key = k;
			hash = h;
		}

		public long getLong() {
			return key;
		}

		@Override
        public Long getKey()
		{
			return key;
		}

		@Override
        public VALUE getValue() {
			return value;
		}

		@Override
        public VALUE setValue(VALUE newValue) {
			VALUE oldValue = value;
			value = newValue;
			return oldValue;
		}

		public boolean equals(Object o) {
			if (!(o instanceof LongEntry))
				return false;
			LongEntry<VALUE> e = (LongEntry<VALUE>)o;
			if (key == e.key) {
				VALUE v1 = getValue();
				VALUE v2 = e.getValue();
				if (v1 == v2 || (v1 != null && v1.equals(v2)))
					return true;
			}
			return false;
		}

		public int hashCode() {
			return hash(key) ^ (value==null ? 0 : value.hashCode());
		}

		public String toString() {
			return String.valueOf(key) + "=" + getValue();
		}

	}

	/**
	 * Add a new entry with the specified key, value and hash code to
	 * the specified bucket.  It is the responsibility of this
	 * method to resize the table if appropriate.
	 *
	 * Subclass overrides this to alter the behavior of put method.
	 */
	void addEntry(int hash, long key, VALUE value, int bucketIndex) {
		table[bucketIndex] = new LongEntry<>(hash, key, value, table[bucketIndex]);
		if (size++ >= threshold)
			resize(2 * table.length);
	}

	/**
	 * Like addEntry except that this version is used when creating entries
	 * as part of Map construction or "pseudo-construction" (cloning,
	 * deserialization).  This version needn't worry about resizing the table.
	 *
	 * Subclass overrides this to alter the behavior of LongKeyMap(Map),
	 * clone, and readObject.
	 */
	void createEntry(int hash, long key, VALUE value, int bucketIndex) {
		table[bucketIndex] = new LongEntry<>(hash, key, value, table[bucketIndex]);
		size++;
	}

	private abstract class HashIterator<X> implements Iterator<X> {
		LongEntry<VALUE> next;                  // next entry to return
		int expectedModCount;        // For fast-fail
		int index;                   // current slot
		LongEntry<VALUE> current;               // current entry

		HashIterator() {
			expectedModCount = modCount;
			LongEntry<VALUE>[] t = table;
			int i = t.length;
			LongEntry<VALUE> n = null;
			if (size != 0) { // advance to first entry
				while (i > 0 && (n = t[--i]) == null)
					;
			}
			next = n;
			index = i;
		}

		@Override
        public boolean hasNext() {
			return next != null;
		}

		LongEntry<VALUE> nextEntry() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			LongEntry<VALUE> e = next;
			if (e == null)
				throw new NoSuchElementException();

			LongEntry<VALUE> n = e.next;
			LongEntry<VALUE>[] t = table;
			int i = index;
			while (n == null && i > 0)
				n = t[--i];
			index = i;
			next = n;
			return current = e;
		}

		@Override
        public void remove() {
			if (current == null)
				throw new IllegalStateException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			long k = current.key;
			current = null;
			LongKeyMap.this.removeEntryForKey(k);
			expectedModCount = modCount;
		}

	}

	private class ValueIterator extends HashIterator<VALUE> {
		@Override
        public VALUE next() {
			return nextEntry().value;
		}
	}

	private class KeyIterator extends HashIterator<Long> {
		@Override
        public Long next() {
			return nextEntry().key;
		}
		public long nextLong() {
			return nextEntry().key;
		}
	}

	private class EntryIterator extends HashIterator<LongEntry<VALUE>> {
		@Override
        public LongEntry<VALUE> next() {
			return nextEntry();
		}
	}

	// Subclass overrides these to alter behavior of views' iterator() method
	Iterator<Long> newKeyIterator()
	{
		return new KeyIterator();
	}
	Iterator<VALUE> newValueIterator()
	{
		return new ValueIterator();
	}
	Iterator<LongEntry<VALUE>> newEntryIterator()
	{
		return new EntryIterator();
	}


	// Views

	private transient Set<LongEntry<VALUE>> entrySet = null;
	transient volatile Set<Long> keySet = null;
	transient volatile Collection<VALUE> values = null;

	/**
	 * Returns a set view of the keys contained in this map.  The set is
	 * backed by the map, so changes to the map are reflected in the set, and
	 * vice-versa.  The set supports element removal, which removes the
	 * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
	 * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
	 * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
	 * <tt>addAll</tt> operations.
	 *
	 * @return a set view of the keys contained in this map.
	 */
	@Override
    public Set<Long> keySet() {
		Set<Long> ks = keySet;
		return (ks != null ? ks : (keySet = new KeySet()));
	}

	private class KeySet extends AbstractSet<Long> {
		@Override
        public Iterator<Long> iterator() {
			return newKeyIterator();
		}
		@Override
        public int size() {
			return size;
		}
		@Override
        public boolean contains(Object o) {
			if (o instanceof Number) {
				return containsKey(((Number)o).longValue());
			} else {
				return false;
			}
		}
		@Override
        public boolean remove(Object o) {
			if (o instanceof Number) {
				return LongKeyMap.this.removeEntryForKey(((Number)o).longValue()) != null;
			} else {
				return false;
			}
		}
		@Override
        public void clear() {
			LongKeyMap.this.clear();
		}
	}

	/**
	 * Returns a collection view of the values contained in this map.  The
	 * collection is backed by the map, so changes to the map are reflected in
	 * the collection, and vice-versa.  The collection supports element
	 * removal, which removes the corresponding mapping from this map, via the
	 * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
	 * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a collection view of the values contained in this map.
	 */
	@Override
    public Collection<VALUE> values() {
		Collection<VALUE> vs = values;
		return (vs != null ? vs : (values = new Values()));
	}

	private class Values extends AbstractCollection<VALUE> {
		@Override
        public Iterator<VALUE> iterator() {
			return newValueIterator();
		}
		@Override
        public int size() {
			return size;
		}
		@Override
        public boolean contains(Object o) {
			return containsValue(o);
		}
		@Override
        public void clear() {
			LongKeyMap.this.clear();
		}
	}

	@Override
    public Set entrySet()
	{
		Set<LongEntry<VALUE>> es = entrySet;
		return (es != null ? es : (entrySet = new EntrySet()));
	}

	private class EntrySet extends AbstractSet<LongEntry<VALUE>> {
		@Override
        public Iterator<LongEntry<VALUE>> iterator() {
			return newEntryIterator();
		}
		@Override
        public boolean contains(Object o) {
			if (!(o instanceof LongEntry))
				return false;
			LongEntry e = (LongEntry)o;
			LongEntry candidate = getEntry(e.key);
			return candidate != null && candidate.equals(e);
		}
		@Override
        public boolean remove(Object o) {
			return removeMapping(o) != null;
		}
		@Override
        public int size() {
			return size;
		}
		@Override
        public void clear() {
			LongKeyMap.this.clear();
		}
	}

	// These methods are used when serializing HashSets
	int   capacity()     { return table.length; }
	float loadFactor()   { return loadFactor;   }
}
