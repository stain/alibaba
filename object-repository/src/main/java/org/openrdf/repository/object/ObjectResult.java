package org.openrdf.repository.object;

import java.util.List;

import org.openrdf.result.Result;
import org.openrdf.store.StoreException;

public interface ObjectResult extends Result<Object> {

	List<String> getBindingNames() throws StoreException;
}
