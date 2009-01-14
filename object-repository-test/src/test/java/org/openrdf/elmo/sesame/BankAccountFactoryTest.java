package org.openrdf.elmo.sesame;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.sesame.BankAccountFactoryTest.BankAccountService.AccountReference;
import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;
import org.openrdf.repository.object.annotations.factory;
import org.openrdf.repository.object.annotations.rdf;

public class BankAccountFactoryTest extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(BankAccountFactoryTest.class);
	}

	@rdf("http://www.example.com/rdf/2008/model#BankAccount")
	public interface BankAccount extends BankAccountBehaviour {
		@rdf("http://www.example.com/rdf/2008/model#accountNumber")
		public long getAccountNumber();

		public void setAccountNumber(long number);
	}

	public interface BankAccountBehaviour {
		public double getBalance();
	}

	@rdf("http://www.example.com/rdf/2008/model#BankAccount")
	public static class BankAccountFactory {
		@factory
		public BankAccountBehaviour createBankAccountBehaviour(
				BankAccount account) {
			BankAccountSupport behaviour = new BankAccountSupport();
			long number = account.getAccountNumber();
			assert number > 0;
			BankAccountService service = BankAccountService.getInstance();
			behaviour.setAccountReference(service.getAccountReference(number));
			behaviour.setBankAccountService(service);
			return behaviour;
		}
	}

	public static class BankAccountService {
		private static BankAccountService instance = new BankAccountService();

		public static BankAccountService getInstance() {
			return instance;
		}

		public class AccountReference {
			private long number;

			public AccountReference(long number) {
				this.number = number;
			}

			@Override
			public int hashCode() {
				return (int) number;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				final AccountReference other = (AccountReference) obj;
				if (number != other.number)
					return false;
				return true;
			}
		}

		private Map<AccountReference, Double> balances = new HashMap<AccountReference, Double>();

		public AccountReference getAccountReference(long number) {
			return new AccountReference(number);
		}

		public double getBalanceOfAccount(AccountReference account) {
			synchronized(balances) {
				return balances.get(account);
			}
		}

		public void setBalanceOfAccount(AccountReference account, double balance) {
			synchronized(balances) {
				balances.put(account, balance);
			}
		}

	}

	public static class BankAccountSupport implements BankAccountBehaviour {
		private AccountReference account;
		private BankAccountService service;

		public double getBalance() {
			return service.getBalanceOfAccount(account);
		}

		public void setAccountReference(AccountReference account) {
			this.account = account;
		}

		public void setBankAccountService(BankAccountService service) {
			this.service = service;
		}
	}

	private static final String NS = "urn:test:";

	public void testBankAccountFactory() throws Exception {
		long number = 10000001;
		BankAccountService service = BankAccountService.getInstance();
		service.setBalanceOfAccount(service.getAccountReference(number), 3225.80);
		QName qname = new QName(NS, Long.toString(number));
		BankAccount account = manager.designate(qname, BankAccount.class);
		account.setAccountNumber(number);
		double balance = account.getBalance();
		assertEquals(3225.80, balance);
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(BankAccount.class);
		module.addFactory(BankAccountFactory.class);
		super.setUp();
	}
}
