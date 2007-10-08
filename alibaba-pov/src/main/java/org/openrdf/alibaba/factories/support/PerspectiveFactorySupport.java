package org.openrdf.alibaba.factories.support;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.factories.DisplayFactory;
import org.openrdf.alibaba.factories.PerspectiveFactory;
import org.openrdf.alibaba.factories.PerspectiveFactoryBehaviour;
import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.formats.Layout;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.owl.DatatypeProperty;
import org.openrdf.concepts.owl.ObjectProperty;
import org.openrdf.concepts.rdf.Seq;
import org.openrdf.concepts.rdfs.Resource;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "PerspectiveFactory")
public class PerspectiveFactorySupport implements PerspectiveFactoryBehaviour {
	private ElmoManager manager;

	private DisplayFactory display;

	public PerspectiveFactorySupport(PerspectiveFactory factory) {
		this.manager = factory.getElmoManager();
		this.display = factory.getPovDisplayFactory();
	}

	public Perspective createPerspectiveFor(Intent intent, Entity target) {
		Perspective spec = manager.create(Perspective.class);
		spec.getPovRepresents().addAll(((Resource) target).getRdfTypes());
		spec.setPovPurpose(intent);
		if (ALI.REFERENCE.equals(intent.getQName())) {
			Format format = (Format) manager.find(ALI.QUALIFIED_NAME);
			Seq<Display> list = manager.create(Seq.class);
			Display qdisplay = display.createDisplay();
			qdisplay.setPovFormat(format);
			list.add(qdisplay);
			spec.setPovDisplays(list);
			spec.setPovLayout((Layout) manager.find(ALI.INLINE));
		} else {
			spec.setPovLayout((Layout) manager.find(ALI.TITLED_LIST));
			spec.setPovDisplays(createDisplayProperties(target.getClass()));
		}
		return spec;
	}

	private Seq<Display> createDisplayProperties(
			java.lang.Class<? extends Entity> type) throws AssertionError {
		Seq<Display> list = manager.create(Seq.class);
		try {
			BeanInfo info = Introspector.getBeanInfo(type);
			for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
				Method getter = pd.getReadMethod();
				if (getter == null)
					continue;
				if (pd.getWriteMethod() == null)
					continue;
				String uri = getPropertyUri(type, getter.getName(), getter
						.getParameterTypes(), new HashSet<Class<?>>());
				if (uri == null)
					continue;
				QName pname = new QName(uri);
				Display d;
				if (isEntityType(pd)) {
					ObjectProperty prop = manager.create(ObjectProperty.class,
							pname);
					d = display.createDisplay(prop);
				} else {
					DatatypeProperty prop = manager.create(
							DatatypeProperty.class, pname);
					d = display.createDisplay(prop);
				}
				list.add(d);
			}
		} catch (IntrospectionException e) {
			throw new AssertionError(e);
		}
		return list;
	}

	private boolean isEntityType(PropertyDescriptor pd) {
		if (Entity.class.isAssignableFrom(pd.getPropertyType()))
			return true;
		if (!Set.class.isAssignableFrom(pd.getPropertyType()))
			return false;
		Method method = pd.getReadMethod();
		Type type = getGenericReturnType(method, method.getDeclaringClass());
		if (type instanceof ParameterizedType) {
			Type param = ((ParameterizedType) type).getActualTypeArguments()[0];
			if (param instanceof Class<?>) {
				return Entity.class.isAssignableFrom((Class<?>) param);
			}
		}
		return false;
	}

	private Type getGenericReturnType(Method method, Class<?> declaring) {
		try {
			String name = method.getName();
			Class<?>[] types = method.getParameterTypes();
			Method m = declaring.getMethod(name, types);
			Type type = m.getGenericReturnType();
			if (type instanceof Class<?> && !declaring.isInterface()) {
				for (Class<?> face : declaring.getInterfaces()) {
					Type t = getGenericReturnType(method, face);
					if (t != null && !(t instanceof Class<?>))
						return t;
				}
			}
			return type;
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	private String getPropertyUri(Class<?> type, String name, Class<?>[] types,
			Set<Class<?>> interfaces) {
		try {
			Method getter = type.getDeclaredMethod(name, types);
			if (getter.isAnnotationPresent(rdf.class)) {
				return getter.getAnnotation(rdf.class).value()[0];
			}
			if (type.isInterface()) {
				String cname = type.getName();
				return "java:" + cname + '#' + type.getName();
			}
		} catch (NoSuchMethodException e) {
		}
		interfaces.add(type);
		for (Class<?> face : type.getInterfaces()) {
			if (interfaces.contains(face))
				continue;
			String uri = getPropertyUri(face, name, types, interfaces);
			if (uri != null)
				return uri;
		}
		return null;
	}
}
