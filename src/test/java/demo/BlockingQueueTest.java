package demo;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

import com.gaea.concurrent.FastBlockingQueue;

/**
 * 调用 BlockingQueue 的put 与 take 进行测试
 * 
 * @author zhenyu.yin
 *
 */
public class BlockingQueueTest {

	public static final int bufferSize = 1024 * 1024;

	// 生产线程数量
	public int wThreadNum = 10;

	// 消费线程数量
	public int rThreadNum = 10;

	// 模拟生产数量
	public int writeNum = 10000000;

	@Test
	public void fastBlockingQueue() throws InterruptedException {
		run(new FastBlockingQueue<Object>(bufferSize));
	}

	@Test
	public void linkedBlockingQueue() throws InterruptedException {
		run(new LinkedBlockingQueue<Object>());
	}

	@Test
	public void arrayBlockingQueue() throws InterruptedException {
		run(new ArrayBlockingQueue<Object>(bufferSize));
	}

	CountDownLatch cd;

	@SuppressWarnings("deprecation")
	public void run(final Queue<Object> queue) throws InterruptedException {
		List<Thread> ts = new LinkedList<Thread>();
		for (int i = 1; i <= wThreadNum; i++) {
			ts.add(new PutThread(i, queue));
		}

		for (int i = 1; i <= rThreadNum; i++) {
			ts.add(new TakeThread(i, queue));
		}
		cd = new CountDownLatch(writeNum);

		long begin = System.currentTimeMillis();
		for (Thread thread : ts) {
			thread.start();
		}
		cd.await();
		long end = System.currentTimeMillis();

		System.out.println(queue.getClass().getSimpleName() + "\tuseTime:" + (end - begin));

		for (Thread thread : ts) {
			thread.stop();
		}
		Thread.sleep(100);
	}

	class TakeThread extends java.lang.Thread {
		private BlockingQueue<Object> queue;

		public TakeThread(int id, Queue<Object> queue) {
			this.queue = (BlockingQueue<Object>) queue;
			setName("RThread-" + id);
		}

		@Override
		public void run() {
			Long take;
			while (true) {
				try {
					take = (Long) queue.take();
					doSomething(take);
					cd.countDown();
				} catch (Exception e) {
				}
			}

		}

		/**
		 * 模拟一些逻辑操作
		 * 
		 * @param take
		 */
		public void doSomething(long take) {
			for (int i = 0; i < 10; i++) {
				Math.cbrt(take);
			}
		}

	}

	class PutThread extends java.lang.Thread {
		BlockingQueue<Object> queue;
		private int id;

		public PutThread(int id, Queue<Object> queue) {
			this.queue = (BlockingQueue<Object>) queue;
			this.id = id;
			setName("WThread-" + id);
		}

		@Override
		public void run() {
			for (long i = id; i <= writeNum; i += wThreadNum) {
				try {
					queue.put(i);
				} catch (Exception e) {
				}
			}
		}
	}

}
