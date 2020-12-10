package hu.montlikadani.tablist.bukkit.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import hu.montlikadani.tablist.bukkit.Perm;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandProcessor {

	/**
	 * @return the name of this command
	 */
	String name() default "";

	/**
	 * @return the permission of this command
	 */
	Perm permission();

	/**
	 * whenever this command should only be performed with player
	 * 
	 * @return <code>false</code> by default
	 */
	boolean playerOnly() default false;
}
