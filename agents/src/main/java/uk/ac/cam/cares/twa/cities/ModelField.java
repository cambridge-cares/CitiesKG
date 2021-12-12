package uk.ac.cam.cares.twa.cities;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ModelField{
	String value();
	boolean backward() default false;
	String graphName() default "";
	/** Required since type erasure prevents the inner class of a list from being accessed at runtime */
	Class<?> innerType() default Model.class;
}