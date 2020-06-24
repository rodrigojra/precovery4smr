package bftsmart.parallelism.late.graph;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import bftsmart.parallelism.late.ConflictDefinition;
import bftsmart.tom.server.defaultservices.CommandsInfo;

public class LockFreeGraphWithDependenciesTest {

	COS cos = null;
	int limit = 1;
	int initialData = 1;

	@Before
	public void setUp() throws Exception {
		System.out.println("===> Setup running");
		limit = 10;
		initialData = 5;
		cos = new LockFreeGraph(limit + initialData);
		CommandsInfo cmdInfo = null;
		
        ConflictDefinition<CommandsInfo> conflictDefinition = new ConflictDefinition<CommandsInfo>() {

			@Override
			public boolean isDependent(CommandsInfo r1, CommandsInfo r2) {
				// TODO Need to find a way to identify dependent commands
				return true;
			}
        };
		
        cos.setConflictDefinition(conflictDefinition);

		for (int i = 0; i < initialData; i++) {
			try {
				cmdInfo = new CommandsInfo(Integer.valueOf(i));
				cos.insert(cmdInfo);	
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail("===> setUp:fail");
			}
		}
	}

//	@Test
//	public void testInsert() {
//		CommandsInfo cmdInfo = null;
//
//		for (int i = initialData; i < limit; i++) {
//			try {
//				cmdInfo = new CommandsInfo(Integer.valueOf(i));
//				cos.insert(cmdInfo);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				fail("LockFreeGraphTest: testInsert: fail");
//			}
//		}
//	}

	@Test
	public void testPrintGraph() {
		LockFreeGraph graph = (LockFreeGraph) cos;
		System.out.println("=================== printing Graph ===============================");
		System.out.println();
		System.out.println(graph.print());
		System.out.println("======================= done Graph ===============================");
/*
 Head  CommandsInfo [ID=0] <0>  CommandsInfo [ID=1] <0>  CommandsInfo [ID=2] <0>  CommandsInfo [ID=3] <0>  CommandsInfo [ID=4] <0>  Tail
*/
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
	public void testGetNext() {
		try {

			Object node = cos.get();
			
			while (node != null) {
				DependencyGraph.vNode vNode = (DependencyGraph.vNode) node;
				CommandsInfo cmdInfo = (CommandsInfo) vNode.getData();
				if (cmdInfo != null) {
					System.out.println("===> testGetNext: " + cmdInfo.getID());	
				}
				
				node = vNode.getNext();
			}
/*
===> testGetNext: 0
===> testGetNext: 1
===> testGetNext: 2
===> testGetNext: 3
===> testGetNext: 4			
 */

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

			System.out.println("===> testRemove: before : " + cosGetResult.getID());
			cos.remove(node);
			node = cos.get();
			cosGetResult = (CommandsInfo) ((DependencyGraph.vNode) node).getData();
			System.out.println("===> testRemove: after: " + cosGetResult.getID());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("===> testGet: fail");
		}
	}
}
