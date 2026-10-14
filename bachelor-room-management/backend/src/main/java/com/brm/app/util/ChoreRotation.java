package com.brm.app.util;

import java.util.List;

/**
 * Pure rotation logic for recurring chores (no Spring/JPA) so it is easy to test.
 * "Order" is the roster of current members in a stable order (join date).
 */
public final class ChoreRotation {

    private ChoreRotation() {
    }

    /**
     * Who gets the task being generated now.
     * <ul>
     *   <li>nobody in the room: null</li>
     *   <li>the chore's "next up" person, if they are still a member</li>
     *   <li>otherwise the first member for rotating chores, or unassigned for fixed ones</li>
     * </ul>
     */
    public static <T> T resolveAssignee(List<T> order, T nextUp, boolean rotate) {
        if (order == null || order.isEmpty()) {
            return null;
        }
        if (nextUp != null && order.contains(nextUp)) {
            return nextUp;
        }
        return rotate ? order.get(0) : null;
    }

    /** The member after {@code current}, wrapping around. Unknown/null current starts at the first member. */
    public static <T> T next(List<T> order, T current) {
        if (order == null || order.isEmpty()) {
            return null;
        }
        int index = current == null ? -1 : order.indexOf(current);
        if (index < 0) {
            return order.get(0);
        }
        return order.get((index + 1) % order.size());
    }
}
