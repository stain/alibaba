package org.openrdf.http.object.mxbeans;

public interface HTTPObjectAgentMXBean {

	String getName();

	void setName(String name);

	String getFrom();

	void setFrom(String from);

	String getStatus();

	ConnectionBean[] getConnections();

	boolean isCacheEnabled();

	void setCacheEnabled(boolean cacheEnabled);

	boolean isCacheAggressive();

	void setCacheAggressive(boolean cacheAggressive);

	boolean isCacheDisconnected();

	void setCacheDisconnected(boolean cacheDisconnected);

	int getCacheCapacity();

	void setCacheCapacity(int capacity);

	int getCacheSize();

	void poke();

	void resetCache() throws Exception;

	void stop() throws Exception;
}
