import org.openrdf.repository.Repository;
import org.openrdf.repository.manager.RepositoryManager;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		RepositoryManager rm = org.openrdf.repository.manager.RepositoryProvider.getRepositoryManager("/tmp");
		Repository foaf = rm.getRepository("foaf");
		System.out.println(foaf.toString());
	}

}
