package hu.montlikadani.tablist.commands;

@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
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
	hu.montlikadani.tablist.Perm permission();

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
