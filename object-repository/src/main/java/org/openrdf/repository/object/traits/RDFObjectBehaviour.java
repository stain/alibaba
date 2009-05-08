package org.openrdf.repository.object.traits;


public interface RDFObjectBehaviour {
	public static final String GET_ENTITY_METHOD = "getBehaviourDelegate";
	public abstract ManagedRDFObject getBehaviourDelegate();
}
