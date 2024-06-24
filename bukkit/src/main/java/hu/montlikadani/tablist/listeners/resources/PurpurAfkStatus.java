package hu.montlikadani.tablist.listeners.resources;

import hu.montlikadani.tablist.utils.Util;
import java.util.logging.Level;

public final class PurpurAfkStatus extends AfkPlayers {

    public PurpurAfkStatus(final hu.montlikadani.tablist.TabList tl, Class<?> afkEvent) {
        java.lang.reflect.Method playerMethod, isGoingAfkMethod;

        try {
            playerMethod = afkEvent.getMethod("getPlayer");
            isGoingAfkMethod = afkEvent.getMethod("isGoingAfk");
        } catch (NoSuchMethodException ex) {
            Util.printTrace(Level.SEVERE, tl, ex.getMessage(), ex);
            return;
        }

        tl.getServer().getPluginManager().registerEvent(afkEvent.asSubclass(org.bukkit.event.Event.class), new org.bukkit.event.Listener() {
        }, org.bukkit.event.EventPriority.NORMAL, (listener, e) -> {
            try {
                goAfk(tl, (org.bukkit.entity.Player) playerMethod.invoke(e), (boolean) isGoingAfkMethod.invoke(e));
            } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException ex) {
                Util.printTrace(Level.SEVERE, tl, ex.getMessage(), ex);
            }
        }, tl);
    }
}
