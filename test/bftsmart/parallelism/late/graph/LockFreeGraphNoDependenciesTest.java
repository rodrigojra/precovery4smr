package bftsmart.parallelism.late.graph;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import bftsmart.tom.server.defaultservices.CommandsInfo;

public class LockFreeGraphNoDependenciesTest {

	COS cos = null;
	int limit = 1;
	int initialData = 0;

	@Before
	public void setUp() throws Exception {
		System.out.println("Setup running");
		//limit = 10;
		//initialData = 5;
		
		cos = new LockFreeGraph(limit + initialData);
		CommandsInfo cmdInfo = null;

		for (int i = 0; i < limit; i++) {
			try {
				cmdInfo = new CommandsInfo(Integer.valueOf(10+i));
				cos.insert(cmdInfo);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail("LockFreeGraphTest:setUp:fail");
			}
		}

	}
/*
	@Test
	public void testInsert() {
		CommandsInfo cmdInfo = null;

		for (int i = initialData; i < limit; i++) {
			try {
				cmdInfo = new CommandsInfo(Integer.valueOf(i));
				cos.insert(cmdInfo);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail("===> testInsert: fail");
			}
		}
	}
*/
	@Test
	public void testPrintGraph() {
		LockFreeGraph graph = (LockFreeGraph) cos;
		System.out.println("printing Graph ==================================================");
		System.out.println();
		System.out.println(graph.print());
		System.out.println("done Graph ==================================================");
	}

	@Test
	public void testGet() {
		try {

			Object node = cos.get();
			CommandsInfo cmdInfo = (CommandsInfo) ((DependencyGraph.vNode) node).getData();
			System.out.println("===> testGet: " + cmdInfo.getID());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("===> testGet: fail");
		}
	}

	@Test
	public void testRemove() {

		try {

			Object node = cos.get();
			CommandsInfo cosGetResult = (CommandsInfo) ((DependencyGraph.vNode) node).getData();

			System.out.println("LockFreeGraphTest: testRemove: before - cosGetResult: " + cosGetResult.getID());
			cos.remove(node);
			node = cos.get();
			cosGetResult = (CommandsInfo) ((DependencyGraph.vNode) node).getData();
			System.out.println("LockFreeGraphTest: testRemove: after - cosGetResult: " + cosGetResult.getID());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("LockFreeGraphTest: testGet: fail");
		}
	}
}
