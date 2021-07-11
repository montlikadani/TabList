package hu.montlikadani.tablist.bukkit.utils.task;

import java.time.Instant;

public final class DelayedPermissionCheck {

	private static final java.util.Set<PermHolder> PERM_HOLDERS = new java.util.HashSet<>();

	public static void clear() {
		PERM_HOLDERS.clear();
	}

	public static boolean hasDelay(String permission, int seconds) {
		PermHolder perm = null;

		for (PermHolder holder : PERM_HOLDERS) {
			if (holder.permission.equals(permission)) {
				perm = holder;
				break;
			}
		}

		if (perm == null) {
			PERM_HOLDERS.add(perm = new PermHolder());
		} else if (perm.checkRate.isAfter(Instant.now())) {
			return true;
		}

		perm.checkRate = Instant.now().plusSeconds(seconds);
		perm.permission = permission;
		return false;
	}

	private static class PermHolder {

		Instant checkRate;
		String permission = "";

	}
}
