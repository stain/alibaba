package org.openrdf.elmo.sesame;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.interceptor.InvocationContext;
import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.intercepts;
import org.openrdf.elmo.annotations.inverseOf;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.sesame.base.RepositoryTestCase;
import org.openrdf.elmo.sesame.behaviours.PropertyChangeNotifierSupport;
import org.openrdf.elmo.sesame.concepts.DcResource;
import org.openrdf.elmo.sesame.concepts.List;
import org.openrdf.elmo.sesame.concepts.Seq;
import org.openrdf.elmo.sesame.roles.PropertyChangeNotifier;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;

public class UserGuideTest extends RepositoryTestCase {
	private static final String NS = "http://www.example.com/rdf/2007/";

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(UserGuideTest.class);
	}

	public interface EmailUser extends User {

		public boolean readEmail(Message message);
	}

	@rdf("http://www.example.com/rdf/2007/Message")
	public static class EmailValidator {
		@intercepts(method = "set.*Email.*", parameters = { String.class })
		public void intercepts(InvocationContext ctx) throws Exception {
			String email = ctx.getParameters()[0].toString();
			if (email.endsWith("@example.com")) {
				ctx.proceed();
			} else {
				throw new IllegalArgumentException("Only internal emails");
			}
		}
	}

	@rdf("http://www.example.com/rdf/2007/Employee")
	public interface Employee {

		public double calculateExpectedBonus(double d);

		@rdf("http://www.example.com/rdf/2007/address")
		public String getAddress();

		@rdf("http://www.example.com/rdf/2007/emailAddress")
		public String getEmailAddress();

		@rdf("http://www.example.com/rdf/2007/name")
		public String getName();

		@rdf("http://www.example.com/rdf/2007/phoneNumber")
		public String getPhoneNumber();

		@rdf("http://www.example.com/rdf/2007/salary")
		public double getSalary();

		public void setAddress(String address);

		public void setEmailAddress(String email);

		public void setName(String string);

		public void setPhoneNumber(String phone);

		public void setSalary(double salary);
	}

	@rdf("http://www.example.com/rdf/2007/Engineer")
	public interface Engineer extends Employee {

		@rdf("http://www.example.com/rdf/2007/bonusTargetMet")
		public boolean isBonusTargetMet();

		public void setBonusTargetMet(boolean met);
	}

	public static abstract class EngineerBonusBehaviour implements Engineer {
		public double calculateExpectedBonus(double percent) {
			boolean target = isBonusTargetMet();
			if (target) {
				return percent * getSalary();
			}
			return 0;
		}
	}

	public static class ITSupportAgent {
		public boolean readEmail(Message message) {
			if (message.getToEmailAddress().equals("help@support.exmple.com")) {
				// process email here
				return true;
			}
			return false;
		}
	}

	@rdf("http://www.example.com/rdf/2007/Message")
	public interface Message {

		@rdf("http://www.example.com/rdf/2007/fromEmailAddress")
		public String getFromEmailAddress();

		@rdf("http://www.example.com/rdf/2007/toEmailAddress")
		public String getToEmailAddress();

		public void setFromEmailAddress(String string);

		public void setToEmailAddress(String string);
	}

	@rdf("http://www.example.com/rdf/2007/Node")
	public interface Node1 {
		@rdf("http://www.example.com/rdf/2007/children")
		public java.util.List<Node1> getChildren();

		public void setChildren(java.util.List<Node1> children);
	}

	@rdf("http://www.example.com/rdf/2007/Node")
	public interface Node2 {
		@rdf("http://www.example.com/rdf/2007/children")
		public java.util.List<Node2> getChildren();

		public void setChildren(java.util.List<Node2> children);
	}

	@rdf("http://www.example.com/rdf/2007/Node")
	public interface Node3 {
		@rdf("http://www.example.com/rdf/2007/child")
		public Set<Node3> getChildren();

		@inverseOf("http://www.example.com/rdf/2007/child")
		public Node3 getParent();

		public void setChildren(Set<Node3> children);
	}

	public static class NodeWithOrderedChildrenSupport {
		private ElmoManager manager;

		public NodeWithOrderedChildrenSupport(Entity bean) {
			manager = bean.getElmoManager();
		}

		@intercepts(method="set.*", parameters={java.util.List.class}, returns=Void.class)
		public void setChildren(InvocationContext ctx) throws Exception {
			java.util.List<?> children = (java.util.List<?>) ctx.getParameters()[0];
			if (children instanceof List) {
				ctx.proceed();
			} else {
				List<Object> seq = manager.create(List.class);
				seq.addAll(children);
				ctx.setParameters(new Object[]{seq});
				ctx.proceed();
			}
		}
	}

	@rdf("http://www.example.com/rdf/2007/Node")
	public interface Node2SetConcept {
		@rdf("http://www.example.com/rdf/2007/child")
		public abstract Set<Node2> getChildSet();

		public abstract void setChildSet(Set<Node2> children);
	}

	public static class NodeWithoutOrderedChildrenSupport implements
			Node2 {
		private Node2SetConcept node;

		public NodeWithoutOrderedChildrenSupport(Node2SetConcept node) {
			this.node = node;
		}
		public java.util.List<Node2> getChildren() {
			return new ArrayList<Node2>(node.getChildSet());
		}

		public void setChildren(java.util.List<Node2> children) {
			node.setChildSet(new HashSet<Node2>(children));
		}
	}

	public static class PersonalBehaviour {
		private EmailUser emailUser;

		public PersonalBehaviour(Entity emailUser) {
			this.emailUser = (EmailUser) emailUser;
		}

		public boolean readEmail(Message message) {
			String un = emailUser.getUserName();
			if (message.getToEmailAddress().equals(un + "@example.com")) {
				// process email here
				return true;
			}
			return false;
		}
	}

	public static final class PropertyChangeListenerImpl implements
			PropertyChangeListener {
		private boolean updated;

		public boolean isUpdated() {
			return updated;
		}

		public void propertyChange(PropertyChangeEvent evt) {
			updated = true;
		}
	}

	@rdf("http://www.example.com/rdf/2007/Salesman")
	public interface Salesman extends Employee {

		@rdf("http://www.example.com/rdf/2007/targetUnits")
		public int getTargetUnits();

		@rdf("http://www.example.com/rdf/2007/unitsSold")
		public int getUnitsSold();

		public void setTargetUnits(int target);

		public void setUnitsSold(int units);
	}

	public static abstract class SalesmanBonusBehaviour implements Salesman {
		public double calculateExpectedBonus(double percent) {
			int units = getUnitsSold();
			int target = getTargetUnits();
			if (units > target) {
				return percent * getSalary() * units / target;
			}
			return 0;
		}
	}

	public interface SupportAgent {
		// Concept identifier
	}

	@rdf("http://www.example.com/rdf/2007/User")
	public interface User {

		@rdf("http://www.example.com/rdf/2007/userName")
		public String getUserName();

		public void setUserName(String name);
	}

	private SesameManagerFactory factory;

	private ElmoManager manager;

	public void testBehaviour1() throws Exception {
		ElmoModule module = new ElmoModule();
		module.addConcept(Node1.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		Node1 node = manager.create(Node1.class);

		// All setter calls use a bean created by the ElmoManager.
		java.util.List<Node1> children = manager.create(Seq.class);

		Node1 childNode = manager.create(Node1.class);
		children.add(childNode);

		node.setChildren(children);

		assertEquals(1, node.getChildren().size());
	}

	public void testBehaviour2() throws Exception {
		ElmoModule module = new ElmoModule();
		module.addConcept(Node2SetConcept.class);
		module.addBehaviour(NodeWithoutOrderedChildrenSupport.class, NS + "Node");
		module.addConcept(Node2.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		Node2 node = manager.create(Node2.class);

		java.util.List<Node2> children = new ArrayList<Node2>();

		Node2 childNode = manager.create(Node2.class);
		children.add(childNode);

		node.setChildren(children);

		assertEquals(1, node.getChildren().size());
	}

	public void testInterceptor2() throws Exception {
		// The RDfBean Seq can also be created within the behaviour.

		ElmoModule module = new ElmoModule();
		module.addBehaviour(NodeWithOrderedChildrenSupport.class, NS + "Node");
		module.addConcept(Node2.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		Node2 node = manager.create(Node2.class);

		java.util.List<Node2> children = new ArrayList<Node2>();

		Node2 childNode = manager.create(Node2.class);
		children.add(childNode);

		node.setChildren(children);

		assertEquals(1, node.getChildren().size());
	}

	public void testChainOfResponsibility() throws Exception {
		String agentType = NS + "SupportAgent";
		String userType = NS + "User";
		ElmoModule module = new ElmoModule();
		module.addBehaviour(ITSupportAgent.class, agentType);
		module.addConcept(SupportAgent.class, agentType);
		module.addBehaviour(PersonalBehaviour.class, userType);
		module.addConcept(EmailUser.class, userType);
		module.addConcept(User.class, userType);
		module.addConcept(Message.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		QName id = new QName(NS, "E340076");
		manager.designate(id, SupportAgent.class);
		manager.designate(id, User.class);

		EmailUser user = (EmailUser) manager.find(id);
		user.setUserName("john");
		Message message = manager.create(Message.class);
		message.setToEmailAddress("john@example.com");
		if (!user.readEmail(message)) {
			fail();
		}
	}

	public void testConcept1() throws Exception {
		ElmoModule module = new ElmoModule();
		module.addConcept(Node3.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		Node3 node = manager.create(Node3.class);

		Set<Node3> children = new HashSet<Node3>();

		Node3 childNode = manager.create(Node3.class);
		assertNull(childNode.getParent());
		children.add(childNode);

		node.setChildren(children);

		assertEquals(1, node.getChildren().size());
		assertEquals(node, childNode.getParent());
	}

	public void testConcept2() throws Exception {
		ElmoModule module = new ElmoModule();
		module.addConcept(Engineer.class, "http://www.example.org/rdf/2007/"
				+ "Engineer");
		// uri type of Salesman is retrieved from the @rdf annotation
		module.addConcept(Salesman.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		Engineer eng = manager.create(Engineer.class);
		assertNotNull(eng);
		Salesman sales = manager.create(Salesman.class);
		assertNotNull(sales);
	}

	public void testContextSpecificData() throws Exception {
		QName c = new QName(NS, "Period#common");
		QName p1 = new QName(NS, "Period#1");
		QName p2 = new QName(NS, "Period#2");
		ElmoModule module = new ElmoModule();
		module.addConcept(Employee.class);
		module.addConcept(Salesman.class);
		module.addConcept(Engineer.class);
		module.addBehaviour(SalesmanBonusBehaviour.class);
		module.addBehaviour(EngineerBonusBehaviour.class);
		module.setGraph(c);
		ElmoModule m1 = new ElmoModule().setGraph(p1).includeModule(module);
		ElmoModule m2 = new ElmoModule().setGraph(p2).includeModule(module);
		factory = new SesameManagerFactory(module, repository);
		SesameManagerFactory f1 = new SesameManagerFactory(m1, repository);
		SesameManagerFactory f2 = new SesameManagerFactory(m2, repository);
		ElmoManager common = factory.createElmoManager();
		ElmoManager period1 = f1.createElmoManager();
		ElmoManager period2 = f2.createElmoManager();
		String qry = "SELECT ?s WHERE { ?j <http://www.example.com/rdf/2007/salary> ?s}";
		try {

			Employee emp;
			Object obj;
			QName id = new QName(NS, "E340076");
			emp = common.designate(id, Employee.class);
			emp.setName("John");
			Salesman slm = period1.designate(id, Salesman.class);
			slm.setTargetUnits(10);
			slm.setUnitsSold(15);
			slm.setSalary(90);
			Engineer eng = period2.designate(id, Engineer.class);
			eng.setBonusTargetMet(true);
			eng.setSalary(100);

			obj = common.find(id);
			assertTrue(obj instanceof Employee);
			assertFalse(obj instanceof Salesman);
			assertFalse(obj instanceof Engineer);
			emp = (Employee) obj;
			assertEquals("John", emp.getName());
			assertEquals(0.0, emp.getSalary(), 0);
			assertTrue(common.createQuery(qry).getResultList().isEmpty());

			obj = period1.find(id);
			assertTrue(obj instanceof Employee);
			assertTrue(obj instanceof Salesman);
			assertFalse(obj instanceof Engineer);
			emp = (Employee) obj;
			assertEquals("John", emp.getName());
			assertEquals(90.0, emp.getSalary(), 0);
			assertEquals(6.75, emp.calculateExpectedBonus(0.05), 0);
			assertEquals(90.0, period1.createQuery(qry).getSingleResult());

			obj = period2.find(id);
			assertTrue(obj instanceof Employee);
			assertFalse(obj instanceof Salesman);
			assertTrue(obj instanceof Engineer);
			emp = (Employee) obj;
			assertEquals("John", emp.getName());
			assertEquals(100.0, emp.getSalary(), 0);
			assertEquals(5, emp.calculateExpectedBonus(0.05), 0);
			assertEquals(100.0, period2.createQuery(qry).getSingleResult());
		} finally {
			common.close();
			period1.close();
			period2.close();
		}
	}

	public void testDataLocalization() throws Exception {
		ElmoManager english;
		ElmoManager french;
		DcResource document;
		factory = new SesameManagerFactory(new ElmoModule(), repository);
		english = factory.createElmoManager(Locale.ENGLISH);
		french = factory.createElmoManager(Locale.FRENCH);
		try {
			QName id = new QName(NS, "D0264967");

			document = (DcResource) english.find(id);
			document.setDcTitle("Elmo User Guide");

			document = (DcResource) french.find(id);
			assertEquals("Elmo User Guide", document.getDcTitle());
			document.setDcTitle("Elmo Guide de l’Utilisateur");
			assertEquals("Elmo Guide de l’Utilisateur", document.getDcTitle());

			english.close();
			english = factory.createElmoManager(Locale.ENGLISH);
			document = (DcResource) english.find(id);
			assertEquals("Elmo User Guide", document.getDcTitle());
		} finally {
			english.close();
			french.close();
		}
	}

	public void testElmoManager1() throws Exception {
		assert Salesman.class.isInterface();
		assert Engineer.class.isInterface();

		ElmoModule module = new ElmoModule();
		module.addConcept(Engineer.class);
		module.addConcept(Salesman.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		QName id = new QName(NS, "E340076");
		manager.designate(id, Salesman.class);
		Object john = manager.designate(id, Engineer.class);

		assertTrue(john instanceof Engineer);
		assertTrue(john instanceof Salesman);
	}

	public void testElmoManager2() throws Exception {
		factory = new SesameManagerFactory(new ElmoModule(), repository);
		manager = factory.createElmoManager();

		String ns = NS;
		QName id = new QName(ns, "E340076");
		Entity john = manager.find(id);

		assertNotNull(john);
		assertEquals(id, john.getQName());

		// the subject john has the uri of
		// "http://www.example.com/rdf/2007/E340076"
	}

	public void testElmoManager3() throws Exception {
		ElmoModule module = new ElmoModule();
		module.addConcept(Employee.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		QName id = new QName(NS, "E340076");
		Employee john = manager.designate(id, Employee.class);
		Employee jonny = (Employee) manager.find(id);

		assert john.equals(jonny);
		assert john != jonny;

		john.setName("John");
		assert jonny.getName().equals("John");
	}

	public void testElmoQuery() throws Exception {
		ElmoModule module = new ElmoModule();
		module.addConcept(Employee.class);
		factory = new SesameManagerFactory(module, repository);
		factory.getRepository().setQueryLanguage(QueryLanguage.SERQL);
		manager = factory.createElmoManager();

		Employee john = manager.create(Employee.class);
		john.setName("John");

		String queryStr = "SELECT emp FROM {emp} <http://www.example.com/rdf/2007/name> {name}";
		ElmoQuery query = manager.createQuery(queryStr);
		query.setParameter("name", "John");
		int count = 0;
		for (Object obj : query.getResultList()) {
			Employee emp = (Employee) obj;
			count++;
			assert emp.getName().equals("John");
		}
		assertEquals(1, count);
	}

	public void testInterceptor1() throws Exception {
		ElmoModule module = new ElmoModule();
		module.addBehaviour(EmailValidator.class);
		module.addConcept(Message.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		Message message = manager.create(Message.class);
		message.setFromEmailAddress("john@example.com"); // okay
		try {
			message.setToEmailAddress("jonny@invalid-example.com");
			fail();
		} catch (IllegalArgumentException e) {
			// invalid email
		}
	}

	public void testLocking() throws Exception {
		repository = new NotifyingRepositoryWrapper(repository, true);
		ElmoModule module = new ElmoModule();
		module.addConcept(Employee.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		for (int i = 0; i < 100; i++) {
			Employee emp = manager.create(Employee.class);
			emp.setName("Emp" + i);
			emp.setAddress(i + " street");
			emp.setPhoneNumber("555-" + i + i);
			emp.setEmailAddress("emp" + i + "@example.com");
		}
		Iterable<Employee> beans = manager.findAll(Employee.class);
		Employee first = beans.iterator().next();
		first.setName(first.getName().replaceAll("Emp", "Employee Number "));
		for (Employee emp : beans) {
			emp.setName(emp.getName().replaceAll("Emp", "Employee Number "));
		}
	}

	public void testObserver() throws Exception {
		repository = new NotifyingRepositoryWrapper(repository, true);
		ElmoModule module = new ElmoModule();
		module.addBehaviour(PropertyChangeNotifierSupport.class, "http://www.example.com/rdf/2007/Employee");
		module.addConcept(Employee.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		QName id = new QName(NS, "E340076");
		Employee employee = manager.designate(id, Employee.class);
		PropertyChangeListenerImpl subscriber = new PropertyChangeListenerImpl();
		((PropertyChangeNotifier) employee)
				.addPropertyChangeListener(subscriber);

		manager.getTransaction().begin();
		employee.setName("john");
		assertFalse(subscriber.isUpdated());
		manager.getTransaction().commit();
		assertTrue(subscriber.isUpdated());
	}

	public void testStrategy() throws Exception {
		ElmoModule module = new ElmoModule();
		module.addBehaviour(SalesmanBonusBehaviour.class);
		module.addBehaviour(EngineerBonusBehaviour.class);
		module.addConcept(Engineer.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		QName id = new QName(NS, "E340076");
		Engineer eng = manager.designate(id, Engineer.class);
		eng.setBonusTargetMet(true);
		eng.setSalary(100);

		Employee employee = (Employee) manager.find(id);
		double bonus = employee.calculateExpectedBonus(0.05);

		assertEquals("bonus", 5.0, bonus, 0);
	}

	@Override
	protected void tearDown() throws Exception {
		if (manager != null)
			manager.close();
		if (factory != null) {
			factory.close();
		}
		super.tearDown();
	}
}
