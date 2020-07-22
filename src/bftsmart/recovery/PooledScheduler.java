package bftsmart.recovery;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import bftsmart.parallelism.late.ConflictDefinition;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.codahale.metrics.MetricRegistry.name;

final class PooledScheduler /* implements Scheduler */ {

    private static final int MAX_SIZE = 150;

    private static final class Task {
        private final Command command;
        private final CompletableFuture<Void> future;

        Task(Command command) {
            this.command = command;
            this.future = new CompletableFuture<>();
        }
    }

	private static class Stats {
        final Counter size;
        final Counter ready;

        Stats(MetricRegistry metrics) {
            size = metrics.counter(name(PooledScheduler.class, "size"));
            ready = metrics.counter(name(PooledScheduler.class, "ready"));
        }
    }

    private final int nThreads;
    private final ExecutorService pool;
    //private final Semaphore space;
    private final List<Task> scheduled;
    //private final Stats stats;

    private Consumer<Command> executor;

    PooledScheduler(int nThreads, ExecutorService pool) {
		this.nThreads = nThreads;
        //this.space = new Semaphore(MAX_SIZE);
        this.scheduled = new LinkedList<>();
        //this.stats = new Stats(metrics);
        this.pool = pool;
    }

    // Breaks cyclic dependency with PooledServiceReplica
    void setExecutor(Consumer<Command> executor) {
        this.executor = executor;
    }
    
//    @Override
//    public int getNumWorkers() {
//        return nThreads;
//    }

//    @Override
    public void schedule(Command command) {
        try {
            //space.acquire();
            //stats.size.inc();
            doSchedule(command);
        } catch (Exception e) {
            // Ignored.
        }
    }

    private void doSchedule(Command command) {
        Task newTask = new Task(command);
        submit(newTask, addTask(newTask));
    }

    private List<CompletableFuture<Void>> addTask(Task newTask) {
        List<CompletableFuture<Void>> dependencies = new LinkedList<>();
        ListIterator<Task> iterator = scheduled.listIterator();
/*
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.future.isDone()) {
                iterator.remove();
                continue;
            }
            if (newTask.command.isDependent(task.command)) {
                dependencies.add(task.future);
            }
        }
*/
        scheduled.add(newTask);
        return dependencies;
    }

    private void submit(Task newTask, List<CompletableFuture<Void>> dependencies) {
        if (dependencies.isEmpty()) {
            //stats.ready.inc();
            pool.execute(() -> execute(newTask));
        } else {
            after(dependencies).thenRunAsync(() -> {
                //stats.ready.inc();
                execute(newTask);
            }, pool);
        }
    }

    private static CompletableFuture<Void> after(List<CompletableFuture<Void>> fs) {
        if (fs.size() == 1) return fs.get(0); // fast path
        return CompletableFuture.allOf(fs.toArray(new CompletableFuture[0]));
    }

    private void execute(Task task) {
        executor.accept(task.command);
        //space.release();
        //stats.ready.dec();
        //stats.size.dec();
        task.future.complete(null);
    }
/*
    @Override
    public ParallelMapping getMapping() {
        return null;
    }

    @Override
    public void scheduleReplicaReconfiguration() {
    }
   */
}

