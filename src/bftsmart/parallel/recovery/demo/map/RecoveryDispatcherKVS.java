package bftsmart.parallel.recovery.demo.map;

import static com.codahale.metrics.MetricRegistry.name;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecoveryDispatcherKVS {

	private static final int MAX_SIZE = 5000;

	private static final class Task {
		private final KeyValueStoreCmd cmd;
		private final CompletableFuture<Void> future;

		Task(KeyValueStoreCmd cmd) {
			this.cmd = cmd;
			this.future = new CompletableFuture<Void>();
		}
	}

	private class Stats {
		final Counter commandWithConflict;
		// final Counter ready;

		Stats(MetricRegistry metrics) {
			commandWithConflict = metrics.counter(name(RecoveryDispatcherKVS.class, "commandWithConflict"));
			// ready = metrics.counter(name(PooledScheduler.class, "ready"));
		}
	}

	private final ExecutorService pool;
	private final Semaphore space;
	private List<Task> scheduled;
	private Stats stats;
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private Consumer<KeyValueStoreCmd> executor;

	public RecoveryDispatcherKVS(/* int nThreads, */ ExecutorService pool, MetricRegistry metrics) {
		// this.nThreads = nThreads;
		this.space = new Semaphore(MAX_SIZE);
		this.scheduled = new LinkedList<>();
		this.stats = new Stats(metrics);
		this.pool = pool;
	}

	// Breaks cyclic dependency with PooledServiceReplica
	public void setExecutor(Consumer<KeyValueStoreCmd> executor) {
		this.executor = executor;
	}

//    @Override
	public void post(KeyValueStoreCmd cmd) {
		try {
			space.acquire();
			// stats.size.inc();
			doSchedule(cmd);
		} catch (Exception e) {
			// Ignored.
		}
	}

	private void doSchedule(KeyValueStoreCmd cmd) {
		Task newTask = new Task(cmd);
		submit(newTask, addTask(newTask));
	}

	private List<CompletableFuture<Void>> addTask(Task newTask) {
		List<CompletableFuture<Void>> dependencies = new LinkedList<>();
		ListIterator<Task> iterator = scheduled.listIterator();

		if (newTask.cmd.getType().isWrite) {
			while (iterator.hasNext()) {
				Task task = iterator.next();
				if (task.future.isDone()) {
					logger.debug(">>> removing task " + task.cmd);
					iterator.remove();
					continue;
				}

				if (newTask.cmd.isDependent(task.cmd)) {
					dependencies.add(task.future);
					logger.debug(">>> dependency added from " + newTask.cmd + "to "+ task.cmd);
					stats.commandWithConflict.inc();
				}
			}
		}
		scheduled.add(newTask);
		return dependencies;
	}

	private void submit(Task newTask, List<CompletableFuture<Void>> dependencies) {
		if (dependencies.isEmpty()) {
			// stats.ready.inc();
			logger.debug(">>> running pool execute "+ newTask.cmd);
			pool.execute(() -> execute(newTask));
		} else {
			logger.debug(">>> running cf.allof() "+ newTask.cmd);
			after(dependencies).thenRun(() -> {
				// stats.ready.inc();
				execute(newTask);
			});
		}
	}

	private static CompletableFuture<Void> after(List<CompletableFuture<Void>> fs) {
		if (fs.size() == 1)
			return fs.get(0); // fast path
		CompletableFuture[] cfDependencies = fs.toArray(new CompletableFuture[fs.size()]);
		CompletableFuture<Void> allFutures = CompletableFuture.allOf(cfDependencies);

		return allFutures;
	}

	private void execute(Task task) {
		executor.accept(task.cmd);
		space.release();
		// stats.ready.dec();
		// stats.size.dec();
		task.future.complete(null);
	}
}
