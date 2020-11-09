package hu.montlikadani.tablist.sponge.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
	/**
	 * The name of the command
	 */
	String name() default "";

	/**
	 * The permission
	 */
	String permission() default "";

	/**
	 * The array of aliases of this command
	 */
	String[] aliases() default {};

	/**
	 * The array of sub commands
	 */
	String[] subCommands() default {};
}
