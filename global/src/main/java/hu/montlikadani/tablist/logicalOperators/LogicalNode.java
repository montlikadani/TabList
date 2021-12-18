package hu.montlikadani.tablist.logicalOperators;

import java.util.concurrent.atomic.AtomicInteger;

public interface LogicalNode {

	static LogicalNode newNode(int type) {
		return new OperatorNodes(type);
	}

	LogicalNode parseInput(String input);

	int getType();

	Condition getCondition();

	boolean parse(double value);

	public static final class NodeType {

		private static final AtomicInteger ATOMIC_PING = new AtomicInteger(1);
		private static final AtomicInteger ATOMIC_TPS = new AtomicInteger(1);

		private NodeType() {
		}

		public static int getLastPing() {
			return ATOMIC_PING.get();
		}

		public static int getLastTps() {
			return ATOMIC_TPS.get();
		}

		public static int getPing() {
			return ATOMIC_PING.getAndSet(ATOMIC_PING.incrementAndGet());
		}

		public static int getTps() {
			return ATOMIC_TPS.getAndSet(ATOMIC_TPS.incrementAndGet());
		}
	}
}
