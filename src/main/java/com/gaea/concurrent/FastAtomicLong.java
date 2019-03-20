package com.gaea.concurrent;

import sun.misc.Unsafe;

class Head {
	protected long p1, p2, p3, p4, p5, p6, p7;
}

class Value extends Head {
	protected volatile long value;
}

/**
 * copy from AtomicLong jdk1.7
 * 
 * @author zhenyu.yin
 *
 */
 final class FastAtomicLong extends Value {
	protected long p11, p12, p13, p14, p15, p16, p17;

	// setup to use Unsafe.compareAndSwapLong for updates
	private static final Unsafe unsafe = Util.getUnsafe();
	private static final long valueOffset;

	/**
	 * Records whether the underlying JVM supports lockless compareAndSwap for
	 * longs. While the Unsafe.compareAndSwapLong method works in either case,
	 * some constructions should be handled at Java level to avoid locking
	 * user-visible locks.
	 */

	static {
		try {
			valueOffset = unsafe.objectFieldOffset(Value.class.getDeclaredField("value"));
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}

	/**
	 * Creates a new AtomicLong with the given initial value.
	 *
	 * @param initialValue
	 *            the initial value
	 */
	public FastAtomicLong(long initialValue) {
		value = initialValue;
	}

	/**
	 * Creates a new AtomicLong with initial value {@code 0}.
	 */
	public FastAtomicLong() {
	}

	/**
	 * Gets the current value.
	 *
	 * @return the current value
	 */
	public final long get() {
		return value;
	}

	/**
	 * Sets to the given value.
	 *
	 * @param newValue
	 *            the new value
	 */
	public final void set(long newValue) {
		value = newValue;
	}

	/**
	 * Eventually sets to the given value.
	 *
	 * @param newValue
	 *            the new value
	 * @since 1.6
	 */
	public final void lazySet(long newValue) {
		unsafe.putOrderedLong(this, valueOffset, newValue);
	}

	/**
	 * Atomically sets to the given value and returns the old value.
	 *
	 * @param newValue
	 *            the new value
	 * @return the previous value
	 */
	public final long getAndSet(long newValue) {
		while (true) {
			long current = get();
			if (compareAndSet(current, newValue))
				return current;
		}
	}

	/**
	 * Atomically sets the value to the given updated value if the current value
	 * {@code ==} the expected value.
	 *
	 * @param expect
	 *            the expected value
	 * @param update
	 *            the new value
	 * @return true if successful. False return indicates that the actual value
	 *         was not equal to the expected value.
	 */
	public final boolean compareAndSet(long expect, long update) {
		return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
	}

	/**
	 * Atomically sets the value to the given updated value if the current value
	 * {@code ==} the expected value.
	 *
	 * <p>
	 * May <a href="package-summary.html#Spurious">fail spuriously</a> and does
	 * not provide ordering guarantees, so is only rarely an appropriate
	 * alternative to {@code compareAndSet}.
	 *
	 * @param expect
	 *            the expected value
	 * @param update
	 *            the new value
	 * @return true if successful.
	 */
	public final boolean weakCompareAndSet(long expect, long update) {
		return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
	}

	/**
	 * Atomically increments by one the current value.
	 *
	 * @return the previous value
	 */
	public final long getAndIncrement() {
		while (true) {
			long current = get();
			long next = current + 1;
			if (compareAndSet(current, next))
				return current;
		}
	}

	/**
	 * Atomically decrements by one the current value.
	 *
	 * @return the previous value
	 */
	public final long getAndDecrement() {
		while (true) {
			long current = get();
			long next = current - 1;
			if (compareAndSet(current, next))
				return current;
		}
	}

	/**
	 * Atomically adds the given value to the current value.
	 *
	 * @param delta
	 *            the value to add
	 * @return the previous value
	 */
	public final long getAndAdd(long delta) {
		while (true) {
			long current = get();
			long next = current + delta;
			if (compareAndSet(current, next))
				return current;
		}
	}

	/**
	 * Atomically increments by one the current value.
	 *
	 * @return the updated value
	 */
	public final long incrementAndGet() {
		for (;;) {
			long current = get();
			if (compareAndSet(current, ++current))
				return current;
		}
	}

	/**
	 * Atomically decrements by one the current value.
	 *
	 * @return the updated value
	 */
	public final long decrementAndGet() {
		for (;;) {
			long current = get();
			if (compareAndSet(current, --current))
				return current;
		}
	}

	/**
	 * Atomically adds the given value to the current value.
	 *
	 * @param delta
	 *            the value to add
	 * @return the updated value
	 */
	public final long addAndGet(long delta) {
		for (;;) {
			long current = get();
			if (compareAndSet(current, current += delta))
				return current;
		}
	}

	/**
	 * Returns the String representation of the current value.
	 * 
	 * @return the String representation of the current value.
	 */
	public String toString() {
		return Long.toString(get());
	}

}
