 package bftsmart.parallel.recovery;

import static com.codahale.metrics.MetricRegistry.name;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import bftsmart.parallel.recovery.demo.counter.CounnterCommand;
import bftsmart.parallel.recovery.demo.counter.CounnterCommand.Type;


public class RecoveryDispatcher {

	private static final class Task {
		private final CounnterCommand counnterCommand;
		private final CompletableFuture<Void> future;

		Task(CounnterCommand counnterCommand) {
			this.counnterCommand = counnterCommand;
			this.future = new CompletableFuture<Void>();
		}
	}

	private class Stats {
		final Counter commandWithConflict;
		//final Counter ready;

		Stats(MetricRegistry metrics) {
			commandWithConflict = metrics.counter(name(RecoveryDispatcher.class, "commandWithConflict"));
			//ready = metrics.counter(name(PooledScheduler.class, "ready"));
		}
	}

	//private final int nThreads;
	private final ExecutorService pool;
	// private final Semaphore space;
	private List<Task> scheduled;
	private Stats stats;

	private Consumer<CounnterCommand> executor;

	public RecoveryDispatcher(/*int nThreads,*/ ExecutorService pool, MetricRegistry metrics) {
		//this.nThreads = nThreads;
		// this.space = new Semaphore(MAX_SIZE);
		this.scheduled = new LinkedList<>();
		this.stats = new Stats(metrics);
		this.pool = pool;
	}

	// Breaks cyclic dependency with PooledServiceReplica
	public void setExecutor(Consumer<CounnterCommand> executor) {
		this.executor = executor;
	}

//    @Override
//    public int getNumWorkers() {
//        return nThreads;
//    }

//    @Override
	public void post(CounnterCommand counnterCommand) {
		try {
			// space.acquire();
			// stats.size.inc();
			doSchedule(counnterCommand);
		} catch (Exception e) {
			// Ignored.
		}
	}

	private void doSchedule(CounnterCommand counnterCommand) {
		Task newTask = new Task(counnterCommand);
		submit(newTask, addTask(newTask));
	}

	private List<CompletableFuture<Void>> addTask(Task newTask) {
		List<CompletableFuture<Void>> dependencies = new LinkedList<>();
		ListIterator<Task> iterator = scheduled.listIterator();

		if (newTask.counnterCommand.getType() == Type.CONFLICT) {
			while (iterator.hasNext()) {
				Task task = iterator.next();
				if (task.future.isDone()) {
					iterator.remove();
					continue;
				}
				
				if (newTask.counnterCommand.isDependent(task.counnterCommand)) {
				//if (task.command.isDependent(newTask.command)) {
					dependencies.add(task.future);
					//System.out.println(">>> dependency added from " + newTask.command + "to "+ task.command);
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
			pool.execute(() -> execute(newTask));
		} else {
			after(dependencies).thenRun(() -> {
				// stats.ready.inc();
				execute(newTask);
			});
		}
	}

	private static CompletableFuture<Void> after(List<CompletableFuture<Void>> fs) {
		if (fs.size() == 1)
			return fs.get(0); // fast path
		return CompletableFuture.allOf(fs.toArray(new CompletableFuture[0]));
	}

	private void execute(Task task) {
		executor.accept(task.counnterCommand);
		// space.release();
		// stats.ready.dec();
		// stats.size.dec();
		task.future.complete(null);
	}

}
