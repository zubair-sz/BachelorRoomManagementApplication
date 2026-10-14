package com.brm.app.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChoreRotationTest {

    private final List<String> roster = List.of("A", "B", "C");

    @Test
    void nextWrapsAroundTheRoster() {
        assertThat(ChoreRotation.next(roster, "A")).isEqualTo("B");
        assertThat(ChoreRotation.next(roster, "C")).isEqualTo("A");
        assertThat(ChoreRotation.next(roster, "gone")).isEqualTo("A");
        assertThat(ChoreRotation.next(roster, null)).isEqualTo("A");
        assertThat(ChoreRotation.next(List.<String>of(), "A")).isNull();
    }

    @Test
    void assigneeFallsBackSensiblyWhenNextUpHasLeft() {
        assertThat(ChoreRotation.resolveAssignee(roster, "B", true)).isEqualTo("B");
        assertThat(ChoreRotation.resolveAssignee(roster, "gone", true)).isEqualTo("A");
        assertThat(ChoreRotation.resolveAssignee(roster, "gone", false)).isNull();
        assertThat(ChoreRotation.resolveAssignee(roster, null, true)).isEqualTo("A");
        assertThat(ChoreRotation.resolveAssignee(List.<String>of(), "A", true)).isNull();
    }

    @Test
    void fiveWeeksOfARotatingChoreStartingAtB() {
        String nextUp = "B";
        StringBuilder weeks = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            String assignee = ChoreRotation.resolveAssignee(roster, nextUp, true);
            weeks.append(assignee);
            nextUp = ChoreRotation.next(roster, assignee);
        }
        assertThat(weeks.toString()).isEqualTo("BCABC");
    }
}
