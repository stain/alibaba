package org.openrdf.repository.object.managers.helpers;

import javassist.bytecode.AccessFlag;

public class CheckForAnnotation extends CheckForConcept {

	public CheckForAnnotation(ClassLoader cl) {
		super(cl);
	}

	protected boolean checkAccessFlags(int flags) {
		return (flags & AccessFlag.ANNOTATION) != 0;
	}
}
