package org.openrdf.repository.object.behaviours;

import junit.framework.Test;

import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;

public class ChainingTest extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(ChainingTest.class);
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(Command.class);
		module.addBehaviour(Command1.class);
		module.addBehaviour(Command2.class);
		super.setUp();
	}

	public static int command = 0;

	@rdf("urn:command")
	public static interface Command {
		public abstract String doCommand();
	}
	
	public static class Command1 implements Command {
		public String doCommand() {
			if (command == 1)
				return "Command 1";
			return null;
		}
	}
	
	public static class Command2 implements Command {
		public String doCommand() {
			if (command == 2)
				return "Command 2";
			return null;
		}
	}
	
	public void testChainCommand() {
		Command cmd = manager.create(Command.class);
		command = 0;
		assertNull(cmd.doCommand());
		command = 1;
		assertEquals("Command 1", cmd.doCommand());
		command = 2;
		assertEquals("Command 2", cmd.doCommand());
	}
}
