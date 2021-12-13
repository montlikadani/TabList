package hu.montlikadani.tablist.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import hu.montlikadani.tablist.Perm;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandProcessor {

	/**
	 * @return the name of this command
	 */
	String name();

	/**
	 * @return the parameters of this command
	 */
	String params() default "";

	/**
	 * @return the permission of this command
	 */
	Perm permission();

	/**
	 * @return the description of this command
	 */
	String desc() default "";

	/**
	 * whenever this command should only be performed by player
	 * 
	 * @return <code>false</code> by default
	 */
	boolean playerOnly() default false;
}
