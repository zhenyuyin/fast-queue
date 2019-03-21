package demo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;

import org.junit.Test;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * 测试 Disruptor 性能
 * 
 * @author zhenyu.yin
 *
 */
public class DisruporTest {
	
	public static final int bufferSize = 1024 * 1024;

	// 生产线程数量
	public static int wThreadNum = 10;

	// 消费线程数量
	public static int rThreadNum = 10;

	// 模拟生产数量
	public static int writeNum = 10000000;
	
	
	/**
	 * BlockingWaitStrategy SleepingWaitStrategy YieldingWaitStrategy
	 */
	public static final WaitStrategy waitStrategy = new YieldingWaitStrategy();

	@Test
	public void run() throws Exception {

		Disruptor<LongEvent> disruptor = new Disruptor<LongEvent>(new MyEventFactory(), bufferSize, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r);
			}

		}, ProducerType.MULTI, waitStrategy);

		@SuppressWarnings("unchecked")
		WorkHandler<LongEvent>[] workers = new WorkHandler[rThreadNum];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new MyWorkHandler();
		}
		disruptor.handleEventsWithWorkerPool(workers);

		disruptor.start();
		final RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();
		cd = new CountDownLatch(writeNum);
		long s1 = System.currentTimeMillis();
		for (int i = 1; i <= wThreadNum; i++) {
			final int id = i;
			new WThread(ringBuffer, id).start();
		}
		cd.await();

		System.out.println("useTime:" + (System.currentTimeMillis() - s1));
		disruptor.shutdown();
	}
	CountDownLatch cd;

	class LongEvent {
		private Object value;

		public Object getValue() {
			return value;
		}

		public void setValue(long value) {
			this.value = value;
		}
	}

	class MyEventFactory implements EventFactory<LongEvent> {
		@Override
		public LongEvent newInstance() {
			return new LongEvent();
		}
	}

	class MyWorkHandler implements WorkHandler<LongEvent> {

		@Override
		public void onEvent(LongEvent event) throws Exception {
			Long value = (Long) event.getValue();
			doSomething(value);
			cd.countDown();
		}

		/**
		 * 模拟一些逻辑操作
		 * 
		 * @param take
		 */
		public void doSomething(long value) {
			for (int i = 0; i < 10; i++) {
				Math.cbrt(value);
			}
		}

	}

	class WThread extends Thread {
		final RingBuffer<LongEvent> ringBuffer;
		final int id;

		public WThread(RingBuffer<LongEvent> ringBuffer, int id) {
			this.ringBuffer = ringBuffer;
			this.id = id;
		}

		@Override
		public void run() {
			for (long l = id; l <= writeNum; l += wThreadNum) {
				final long sequence = ringBuffer.next();
				try {
					ringBuffer.get(sequence).setValue(l);
				} finally {
					ringBuffer.publish(sequence);
				}
			}
		}
	}
}
