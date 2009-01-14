package javax.interceptor;

import java.lang.reflect.Method;
import java.util.Map;

public interface InvocationContext {

	public abstract Object getTarget();

	public abstract Method getMethod();

	public abstract Object[] getParameters();

	public abstract void setParameters(Object[] params);

	public abstract Map getContextData();

	public abstract Object proceed() throws Exception;
}
