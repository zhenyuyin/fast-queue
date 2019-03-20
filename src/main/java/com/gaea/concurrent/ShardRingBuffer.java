package com.gaea.concurrent;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.LockSupport;

final class ShardRingBuffer<T> {
	
	public static final int DEFAULT_BUFFER_LEVEL = 15;

	private final int shard;
	private final int shardMask;
	private final int shardBit;
	private final int indexMask;
	private final AtomicReferenceArray<T>[] nodes;

	@SuppressWarnings("unchecked")
	public ShardRingBuffer(final int size) {
		if (size < 1) {
			throw new IllegalArgumentException("bufferSize must not be less than 1");
		}

		if (size > FastQueue.MAX_SIZE) {
			throw new IllegalArgumentException("bufferSize must not be less than " + FastQueue.MAX_SIZE);
		}

		if (Integer.bitCount(size) != 1) {
			throw new IllegalArgumentException("bufferSize must be a power of 2");
		}
		this.shard = Math.max(Integer.highestOneBit(size >> DEFAULT_BUFFER_LEVEL), 1);
		this.shardMask = size - 1;
		this.indexMask = size / shard - 1;
		this.shardBit = Integer.bitCount(indexMask);
		this.nodes = new AtomicReferenceArray[shard];
		for (int i = 0; i < shard; i++) {
			this.nodes[i] = new AtomicReferenceArray<T>(size / shard);
		}
	}

	public void put(final long index, final T t) {
		final int dataIndex = index(index);
		final AtomicReferenceArray<T> node = getNode(index);
		while (!node.compareAndSet(dataIndex, null, t)) {
			LockSupport.parkNanos(1); // 队列满了,for CAS, 尽可能让出CPU给消费线程
		}
	}

	public boolean offer(final long index, final T t) {
		final int dataIndex = index(index);
		final AtomicReferenceArray<T> node = getNode(index);
		return node.compareAndSet(dataIndex, null, t);
	}

	public T take(final long index) {
		final int dataIndex = index(index);
		final AtomicReferenceArray<T> node = getNode(index);
		T t;
		while ((t = node.getAndSet(dataIndex, null)) == null) {
			Thread.yield(); // 生产者申请了节点,但还没更新元素, for CAS
		}
		return t;
	}

	public T get(final long index) {
		final int dataIndex = index(index);
		return getNode(index).get(dataIndex);
	}

	public boolean compareAndSet(final long index, final T old, final T update) {
		final int dataIndex = index(index);
		final AtomicReferenceArray<T> node = getNode(index);
		return node.compareAndSet(dataIndex, old, update);
	}

	private AtomicReferenceArray<T> getNode(final long index) {
		return nodes[nodeIndex(index)];
	}

	private int index(final long next) {
		return (int) (next & indexMask);
	}

	private int nodeIndex(final long next) {
		return (int) (next & shardMask) >> shardBit;
	}
}
