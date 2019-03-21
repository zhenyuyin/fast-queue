package com.gaea.concurrent;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 
 * @author zhenyu.yin
 *
 */
public class FastQueue<T> extends AbstractQueue<T> {
	public static final int MAX_SIZE = 1 << 30;

	protected final ShardRingBuffer<T> buffer;
	protected final int capacity;
	protected final FastAtomicLong head = new FastAtomicLong();
	protected final FastAtomicLong tail = new FastAtomicLong();
	protected long headcache = 0;

	  /**
     * Creates an {@code FastQueue} with the given (fixed)
     * capacity and default access policy.
     *
     * @param capacity the capacity of this queue .the capacity will be corrected to 2 to the N
     * @throws IllegalArgumentException if {@code bufferSize < 1 or bufferSize < MAX_SIZE}
     */
	public FastQueue(int capacity) {
		if (capacity < 1) {
			throw new IllegalArgumentException("capacity must not be less than 1");
		}

		if (capacity > MAX_SIZE) {
			throw new IllegalArgumentException("capacity must not be less than " + MAX_SIZE);
		}
		if (Integer.bitCount(capacity) != 1) {
			capacity = Integer.highestOneBit(capacity) << 1;
		}
		this.capacity = capacity;
		buffer = new ShardRingBuffer<>(capacity);
	}

	@Override
	public boolean offer(final T t) {
		checkNotNull(t);
		long next;

		do {
			next = head.get();
			if (buffer.get(next) != null && head.get() == next) {
				return false;
			}
		} while (!head.compareAndSet(next, next + 1));
		
		buffer.put(next, t);
		return true;
	}

	@Override
	public T peek() {
		long next = tail.get();
		if (!canTake(next)) {
			return null;
		}
		return buffer.get(next);
	}

	@Override
	public T poll() {
		long next;
		do {
			if (!canTake(next = tail.get())) {
				return null;
			}
		} while (!tail.compareAndSet(next, next + 1));

		return buffer.take(next);
	}

	protected final boolean canTake(final long next) {
		return headcache > next || (headcache = head.get()) > next;
	}

	@Override
	public int size() {
		return (int) Math.max(head.get() - tail.get(), 0);
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean contains(final Object o) {
		for (long next = tail.get(); next < head.get(); next++) {
			if (o.equals(buffer.get(next))) {
				return true;
			}
		}
		return false;
	}

	private final class Itr implements Iterator<T> {

		long next = 0;
		long index;
		T last;

		public Itr() {
			initNext();
		}

		@Override
		public boolean hasNext() {
			return last != null;
		}

		@Override
		public T next() {
			if (last == null) {
				throw new NoSuchElementException();
			}
			T _t = last;
			index = next++;
			last = null;
			initNext();
			return _t;
		}

		public void initNext() {
			final long l = head.get();
			next = Math.max(next, tail.get());
			while (next < l) {
				last = buffer.get(next);
				if (last != null) {
					break;
				}
			}
		}

		@Override
		public void remove() {
			if (last == null) {
				throw new IllegalStateException();
			}
			buffer.compareAndSet(index, last, null);
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new Itr();
	}

	static void checkNotNull(final Object v) {
		if (v == null)
			throw new NullPointerException();
	}

	@Override
	public Object[] toArray() {
		List<T> list = toList();
		return list.toArray();
	}

	private final List<T> toList() {
		List<T> list = new LinkedList<T>();
		long current = head.get();
		for (long i = tail.get(); i < current; i++) {
			T data = buffer.get(i);
			if (data != null) {
				list.add(data);
			}
		}
		return list;
	}

	@SuppressWarnings({ "hiding", "unchecked" })
	@Override
	public <T> T[] toArray(final T[] a) {
		List<T> list = new LinkedList<T>();
		long current = head.get();
		for (long i = tail.get(); i < current; i++) {
			T data = (T) buffer.get(i);
			if (data != null) {
				list.add(data);
			}
		}
		return list.toArray(a);
	}

	@Override
	public boolean remove(final Object o) {
		checkNotNull(o);
		T t;
		for (long i = tail.get(); i < head.get(); i++) {
			t = buffer.get(i);
			if (o.equals(t)) {
				return buffer.compareAndSet(i, t, null);
			}
		}
		return false;
	}

}
