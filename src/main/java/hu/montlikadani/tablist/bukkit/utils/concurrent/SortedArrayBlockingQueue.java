package hu.montlikadani.tablist.bukkit.utils.concurrent;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;

import org.bukkit.Bukkit;

/**
 * This class shouldn't be used as API. It is not guaranteed to work properly.
 * 
 * @param <E>
 */
public final class SortedArrayBlockingQueue<E> extends ArrayBlockingQueue<E> {

	private static final long serialVersionUID = 1L;

	private Object[] objects;

	public SortedArrayBlockingQueue(int count) {
		super(count);

		objects = new Object[count];
	}

	/**
	 * Returns the copy of array which contains the cached elements.
	 * 
	 * @return the copied array
	 */
	public final Object[] getObjects() {
		return copyOf(objects.length);
	}

	/**
	 * Inserts the given element at the given index.
	 * <p>
	 * <b>This does not change the value of the array in {@link ArrayBlockingQueue},
	 * nor does it increase its size. Creates a cached array that will store the
	 * added elements, which can be modified based on an index.</b>
	 * 
	 * @param index the index where to insert
	 * @param e     the element to add
	 */
	public final void add(int index, E e) {
		if (index < 0) {
			return;
		}

		if (add(e)) {
			if (index > objects.length) {
				objects = copyOf(index);
			}

			if (objects.length == 0) {
				objects = new Object[Bukkit.getOnlinePlayers().size() + 1];
			}

			objects[index] = e;
		}
	}

	@Override
	public boolean remove(Object e) {
		if (super.remove(e)) {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] == e) {
					objects[i] = null;
					clean();
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public Iterator<E> iterator() {
		return new IteratorArray(objects);
	}

	private final void clean() {
		java.util.List<Object> list = new java.util.ArrayList<>();

		for (Object obj : objects) {
			if (obj != null) {
				list.add(obj);
			}
		}

		objects = list.toArray();
	}

	private final Object[] copyOf(int size) {
		return Arrays.copyOf(objects, size < 1 ? objects.length + 1 : size);
	}

	private final class IteratorArray implements Iterator<E> {

		private Object array;

		private int endIndex;
		private int index = 0;

		public IteratorArray(Object array) {
			super();

			this.array = array;
			endIndex = Array.getLength(array);
			index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < endIndex;
		}

		@Override
		@SuppressWarnings("unchecked")
		public E next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			return (E) Array.get(array, index++);
		}
	}
}
