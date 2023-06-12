package hu.montlikadani.tablist.listeners.resources;

public final class PurpurAfkStatus extends AfkPlayers {

    public PurpurAfkStatus(final hu.montlikadani.tablist.TabList tl, Class<?> afkEvent) {
        java.lang.reflect.Method playerMethod, isGoingAfkMethod;

        try {
            playerMethod = afkEvent.getDeclaredMethod("getPlayer");
            isGoingAfkMethod = afkEvent.getDeclaredMethod("isGoingAfk");
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
            return;
        }

        tl.getServer().getPluginManager().registerEvent(afkEvent.asSubclass(org.bukkit.event.Event.class), new org.bukkit.event.Listener() {
        }, org.bukkit.event.EventPriority.NORMAL, (listener, e) -> {
            try {
                goAfk(tl, (org.bukkit.entity.Player) playerMethod.invoke(e), (boolean) isGoingAfkMethod.invoke(e));
            } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }, tl);
    }
}
