package com.gaea.concurrent;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author zhenyu.yin
 *
 */
public class FastBlockingQueue<T> extends FastQueue<T> implements BlockingQueue<T> {

	private int wait = 0;

	/**
	 * Creates an {@code FastBlockingQueue} with the given (fixed)
	 *
	 * @param capacity
	 *            the capacity of this queue .the capacity will be corrected to 2 to the N
	 * @throws IllegalArgumentException  if {@code bufferSize < 1 or bufferSize < MAX_SIZE}
	 */
	public FastBlockingQueue(final int capacity) {
		super(capacity);
	}

	@Override
	public void put(final T t) throws InterruptedException {
		checkNotNull(t);
		final long next = head.getAndIncrement();
		buffer.put(next, t);
		checkNotify();
	}

	private final void checkNotify() {
		if (wait == 0) {
			return;
		}
		synchronized (head) {
			head.notify();
		}
	}

	@Override
	public boolean offer(final T t) {
		final boolean suc = super.offer(t);
		if (suc) {
			checkNotify();
		}
		return suc;
	}

	@Override
	public T take() throws InterruptedException {
		long next;
		do {
			if (!canTake(next = tail.get())) {
				waitNext(next);
			}
		} while (!tail.compareAndSet(next, next + 1));

		return buffer.take(next);
	}

	private final long waitNext(final long next) throws InterruptedException {
		synchronized (head) {
			wait++;
			do {
				head.wait();
			} while (next >= (headcache = head.get()));
			wait--;
		}
		return headcache;
	}

	@Override
	public int remainingCapacity() {
		return super.capacity - size();
	}

	@Override
	public T poll(final long timeout, final TimeUnit unit) {
		final long now = System.nanoTime();
		final long nanos = unit.toNanos(timeout);
		long next;
		do {
			if (System.nanoTime() - now > nanos) {
				return null;
			}
			next = tail.get();
			if (!canTake(next)) {
				return null;
			}
		} while (!tail.compareAndSet(next, next + 1));

		return buffer.take(next);
	}

	@Override
	public boolean offer(final T t, final long timeout, final TimeUnit unit) {
		final long now = System.nanoTime();
		final long nanos = unit.toNanos(timeout);
		do {
			if (super.offer(t)) {
				return true;
			}
		} while (System.nanoTime() - now < nanos);
		return false;
	}

	@Override
	public int drainTo(final Collection<? super T> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}

	@Override
	public int drainTo(final Collection<? super T> c, final int maxElements) {
		checkNotNull(c);
		if (c == this)
			throw new IllegalArgumentException();
		if (maxElements <= 0)
			return 0;
		int count = 0;

		long nextIndex, headIndex;
		do {
			nextIndex = tail.get();
			headIndex = Math.min(head.get(), nextIndex + maxElements);
		} while (!tail.compareAndSet(nextIndex, headIndex));
		T t;
		while (nextIndex + count < headIndex) {
			t = buffer.take(nextIndex + count++);
			c.add(t);
		}
		return count;
	}

}
