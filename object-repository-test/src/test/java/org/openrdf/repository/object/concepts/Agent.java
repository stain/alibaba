package org.openrdf.repository.object.concepts;

import java.util.Set;

import org.openrdf.repository.object.annotations.rdf;

/** An agent (eg. person, group, software or physical artifact). */
@rdf("urn:foaf:Agent")
public interface Agent {


	/** An AIM chat ID */
	@rdf("urn:foaf:aimChatID")
	public abstract Set<Object> getFoafAimChatIDs();

	/** An AIM chat ID */
	public abstract void setFoafAimChatIDs(Set<Object> value);


	/** The  birthday of this Agent, represented in mm-dd string form, eg. '12-31'. */
	@rdf("urn:foaf:birthday")
	public abstract Object getFoafBirthday();

	/** The  birthday of this Agent, represented in mm-dd string form, eg. '12-31'. */
	public abstract void setFoafBirthday(Object value);


	/** An organization funding a project or person. */
	@rdf("urn:foaf:fundedBy")
	public abstract Set<Object> getFoafFundedBy();

	/** An organization funding a project or person. */
	public abstract void setFoafFundedBy(Set<Object> value);


	/** The gender of this Agent (typically but not necessarily 'male' or 'female'). */
	@rdf("urn:foaf:gender")
	public abstract Object getFoafGender();

	/** The gender of this Agent (typically but not necessarily 'male' or 'female'). */
	public abstract void setFoafGender(Object value);


	/** An ICQ chat ID */
	@rdf("urn:foaf:icqChatID")
	public abstract Set<Object> getFoafIcqChatIDs();

	/** An ICQ chat ID */
	public abstract void setFoafIcqChatIDs(Set<Object> value);


	/** A jabber ID for something. */
	@rdf("urn:foaf:jabberID")
	public abstract Set<Object> getFoafJabberIDs();

	/** A jabber ID for something. */
	public abstract void setFoafJabberIDs(Set<Object> value);


	/** A logo representing some thing. */
	@rdf("urn:foaf:logo")
	public abstract Set<Object> getFoafLogos();

	/** A logo representing some thing. */
	public abstract void setFoafLogos(Set<Object> value);


	/** Something that was made by this agent. */
	@rdf("urn:foaf:made")
	public abstract Set<Object> getFoafMades();

	/** Something that was made by this agent. */
	public abstract void setFoafMades(Set<Object> value);


	/** An agent that made this thing. */
	@rdf("urn:foaf:maker")
	public abstract Set<Agent> getFoafMakers();

	/** An agent that made this thing. */
	public abstract void setFoafMakers(Set<Agent> value);


	/** A personal mailbox, ie. an Internet mailbox associated with exactly one owner, the first owner of this mailbox. This is a 'static inverse functional property', in that  there is (across time and change) at most one individual that ever has any particular value for foaf:mbox. */
	@rdf("urn:foaf:mbox")
	public abstract Set<Object> getFoafMboxes();

	/** A personal mailbox, ie. an Internet mailbox associated with exactly one owner, the first owner of this mailbox. This is a 'static inverse functional property', in that  there is (across time and change) at most one individual that ever has any particular value for foaf:mbox. */
	public abstract void setFoafMboxes(Set<Object> value);


	/** The sha1sum of the URI of an Internet mailbox associated with exactly one owner, the  first owner of the mailbox. */
	@rdf("urn:foaf:mbox_sha1sum")
	public abstract Set<Object> getFoafMbox_sha1sums();

	/** The sha1sum of the URI of an Internet mailbox associated with exactly one owner, the  first owner of the mailbox. */
	public abstract void setFoafMbox_sha1sums(Set<Object> value);


	/** An MSN chat ID */
	@rdf("urn:foaf:msnChatID")
	public abstract Set<Object> getFoafMsnChatIDs();

	/** An MSN chat ID */
	public abstract void setFoafMsnChatIDs(Set<Object> value);


	/** A name for some thing. */
	@rdf("urn:foaf:name")
	public abstract Set<Object> getFoafNames();

	/** A name for some thing. */
	public abstract void setFoafNames(Set<Object> value);


	/** A theme. */
	@rdf("urn:foaf:theme")
	public abstract Set<Object> getFoafThemes();

	/** A theme. */
	public abstract void setFoafThemes(Set<Object> value);


	/** A Yahoo chat ID */
	@rdf("urn:foaf:yahooChatID")
	public abstract Set<Object> getFoafYahooChatIDs();

	/** A Yahoo chat ID */
	public abstract void setFoafYahooChatIDs(Set<Object> value);

}
