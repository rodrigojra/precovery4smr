package bftsmart.parallel.recovery.demo.map;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.codahale.metrics.Timer;

import com.codahale.metrics.MetricRegistry;
import static com.codahale.metrics.MetricRegistry.name;

import bftsmart.tom.ServiceProxy;

public class KeyValueStoreClient extends Thread {

	ServiceProxy proxy;
	//private final ParallelServiceProxy proxy;
	private static final Logger LOGGER = Logger.getLogger(KeyValueStoreClient.class.getName());
    
    private final Timer requestTimer;
    private final int opsPerRequest;
    private final int maxKey;
    private final float keySparseness;
    private final float conflictPercentage;

	private KeyValueStoreClient(int clientID,
            int opsPerRequest,
            int maxKey,
            float keySparseness,
            float conflictPercentage,
            MetricRegistry metrics) {
		//super("KeyValueStoreClient - " + clientID);
		//this.proxy = new ParallelServiceProxy(clientID);
		this.proxy = new ServiceProxy(clientID);
		this.opsPerRequest = opsPerRequest;
		this.maxKey = maxKey;
		this.keySparseness = keySparseness;
		this.conflictPercentage = conflictPercentage;
		this.requestTimer = metrics.timer(name(KeyValueStoreClient.class, "requests"));
}

	public static void main(String[] args) {
		if (args.length != 8) {
			System.out.println(
					"Usage: KeyValueStoreClient " 
							+ "<process id> "
							+ "<threads> " 
							+ "<ops per request> " 
							+ "<max key> "
							+ "<duration sec> " 
							+ "<key sparseness> " 
							+ "<conflict percentage> " 
							+ "<log metrics?>");
			System.exit(1);
		}

		try {
			LOGGER.info("process id " + args[0]);
			LOGGER.info("threads " + args[1]);
			LOGGER.info("ops per request " + args[2]);
			LOGGER.info("max key " + args[3]);
			LOGGER.info("duration sec" + args[4]);
			LOGGER.info("key sparseness" + args[5]);
			LOGGER.info("conflict percentage" + args[6]);
			LOGGER.info("log metrics?" + args[7]);
			
			runWorkload(
					Integer.parseInt(args[0]), 
					Integer.parseInt(args[1]), 
					Integer.parseInt(args[2]),
					Integer.parseInt(args[3]), 
					Integer.parseInt(args[4]), 
					Float.parseFloat(args[5]),
					Float.parseFloat(args[6]), 
					Boolean.parseBoolean(args[7]), 
					null);
			
		} catch (NumberFormatException e) {
			LOGGER.log(Level.SEVERE, "Invalid arguments", e);
			System.exit(1);
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Experiment interrupted", e);
			System.exit(1);
		}

		LOGGER.info("Experiment completed.");
		System.exit(0);
	}

	private static void runWorkload(
			int processID, 
			int nThreads, 
			int opsPerRequest, 
			int maxKey, 
			int durationSec,
			float keySparseness, 
			float conflictPercentage, 
			boolean logMetrics, 
			File metricsPath)
			throws InterruptedException {

		for (int i = 0; i < nThreads; i++) {
			new KeyValueStoreClient(processID + i, 
					opsPerRequest, 
					maxKey, 
					keySparseness, 
					conflictPercentage, 
					null)
			.start();
		}
	}
}