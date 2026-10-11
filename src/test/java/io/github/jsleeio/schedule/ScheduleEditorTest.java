package io.github.jsleeio.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScheduleEditorTest {

    private static final List<Activity> BASE = List.of(Activity.work("상담", "09:00", "18:00"));

    private static final List<Activity> WITH_LUNCH = List.of(
            Activity.work("상담", "09:00", "12:00"),
            Activity.rest("점심", "12:00", "13:00"),
            Activity.work("상담", "13:00", "18:00"));

    @Test
    @DisplayName("기본 구간 중간에 활동을 넣으면 앞뒤 시간이 원래 활동으로 남는다")
    void splitsBaseActivity() {
        List<Activity> result = ScheduleEditor.apply(BASE, Activity.rest("점심", "12:00", "13:00"));

        assertSchedule(result,
                "09:00-12:00 상담",
                "12:00-13:00 점심",
                "13:00-18:00 상담");
    }

    @Test
    @DisplayName("여러 활동에 걸친 신규 활동은 걸친 활동을 각각 잘라낸다")
    void cutsEveryOverlappedActivity() {
        List<Activity> result = ScheduleEditor.apply(WITH_LUNCH, Activity.work("회의", "11:30", "12:30"));

        assertSchedule(result,
                "09:00-11:30 상담",
                "11:30-12:30 회의",
                "12:30-13:00 점심",
                "13:00-18:00 상담");
    }

    @Test
    @DisplayName("시작 시각이 같으면 기존 활동을 대체하고 뒤쪽 남은 시간은 원래 활동으로 남는다")
    void sameStartReplacesAndKeepsTail() {
        List<Activity> schedule = List.of(
                Activity.work("상담", "09:00", "10:00"),
                Activity.work("회의", "10:00", "12:00"),
                Activity.work("상담", "12:00", "18:00"));

        List<Activity> result = ScheduleEditor.apply(schedule, Activity.work("코칭", "10:00", "11:00"));

        assertSchedule(result,
                "09:00-10:00 상담",
                "10:00-11:00 코칭",
                "11:00-12:00 회의",
                "12:00-18:00 상담");
    }

    @Test
    @DisplayName("신규 활동이 기존 활동을 완전히 덮으면 기존 활동은 사라진다")
    void fullyCoveredActivityIsRemoved() {
        List<Activity> result = ScheduleEditor.apply(WITH_LUNCH, Activity.work("회의", "11:30", "13:30"));

        assertSchedule(result,
                "09:00-11:30 상담",
                "11:30-13:30 회의",
                "13:30-18:00 상담");
    }

    @Test
    @DisplayName("경계만 맞닿는 활동은 잘리지 않는다")
    void touchingBoundaryIsNotCut() {
        List<Activity> result = ScheduleEditor.apply(WITH_LUNCH, Activity.work("회의", "11:00", "12:00"));

        assertSchedule(result,
                "09:00-11:00 상담",
                "11:00-12:00 회의",
                "12:00-13:00 점심",
                "13:00-18:00 상담");
    }

    @Test
    @DisplayName("결과 근무표는 빈틈이나 겹침 없이 처음과 끝이 유지된다")
    void resultStaysContinuous() {
        List<Activity> result = ScheduleEditor.apply(WITH_LUNCH, Activity.work("회의", "11:30", "12:30"));
        result = ScheduleEditor.apply(result, Activity.work("회의", "15:00", "16:00"));

        assertEquals("09:00", Activity.toText(result.get(0).start()));
        assertEquals("18:00", Activity.toText(result.get(result.size() - 1).end()));
        for (int i = 1; i < result.size(); i++) {
            assertEquals(result.get(i - 1).end(), result.get(i).start(), "구간 사이에 빈틈이나 겹침이 있음");
        }
    }

    @Test
    @DisplayName("입력한 근무표 리스트는 바뀌지 않는다")
    void inputIsNotModified() {
        List<Activity> input = new ArrayList<>(WITH_LUNCH);

        ScheduleEditor.apply(input, Activity.work("회의", "11:30", "12:30"));

        assertEquals(WITH_LUNCH, input);
    }

    @Test
    @DisplayName("시작과 종료가 같거나 거꾸로면 활동을 만들 수 없다")
    void rejectsInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> Activity.work("회의", "10:00", "10:00"));
        assertThrows(IllegalArgumentException.class, () -> Activity.work("회의", "11:00", "10:00"));
    }

    @Test
    @DisplayName("빈 근무표에 넣은 활동은 그대로 신규 구간이 된다")
    void emptyScheduleAddsNewActivity() {
        List<Activity> result = ScheduleEditor.apply(List.of(), Activity.work("상담", "09:00", "18:00"));

        assertSchedule(result, "09:00-18:00 상담");
    }

    @Test
    @DisplayName("기본 근무 밖에 넣은 연장근무는 신규 구간으로 추가된다")
    void overtimeOutsideBaseIsAdded() {
        List<Activity> result = ScheduleEditor.apply(BASE, Activity.work("연장근무", "18:00", "19:00"));

        assertSchedule(result,
                "09:00-18:00 상담",
                "18:00-19:00 연장근무");
    }

    @Test
    @DisplayName("근무표 중간의 빈 시간은 건드리지 않는다")
    void gapInScheduleIsKept() {
        List<Activity> schedule = List.of(
                Activity.work("상담", "09:00", "11:10"),
                Activity.rest("점심", "11:30", "12:30"),
                Activity.work("상담", "12:30", "18:00"));

        List<Activity> result = ScheduleEditor.apply(schedule, Activity.work("회의", "10:00", "10:30"));

        assertSchedule(result,
                "09:00-10:00 상담",
                "10:00-10:30 회의",
                "10:30-11:10 상담",
                "11:30-12:30 점심",
                "12:30-18:00 상담");
    }

    @Test
    @DisplayName("시간을 넓혀 넣은 활동은 범위 안의 기존 구간을 모두 정리한다")
    void widenedActivityClearsEveryRowInRange() {
        List<Activity> schedule = List.of(
                Activity.work("상담", "09:00", "10:00"),
                Activity.work("회의", "10:00", "11:00"),
                Activity.work("상담", "11:00", "18:00"));

        List<Activity> result = ScheduleEditor.apply(schedule, Activity.work("코칭", "09:00", "12:00"));

        assertSchedule(result,
                "09:00-12:00 코칭",
                "12:00-18:00 상담");
    }

    @Test
    @DisplayName("자정을 넘는 야간 근무도 날짜 기준으로 순서가 유지된다")
    void overnightScheduleKeepsOrder() {
        List<Activity> night = List.of(Activity.work("상담", "22:00", "익일 07:00"));

        List<Activity> result = ScheduleEditor.apply(night, Activity.rest("휴게", "익일 02:00", "익일 03:00"));

        assertSchedule(result,
                "22:00-익일 02:00 상담",
                "익일 02:00-익일 03:00 휴게",
                "익일 03:00-익일 07:00 상담");
    }

    @Test
    @DisplayName("원래 활동과 같은 활동을 그 자리에 다시 넣으면 막는다")
    void rejectsSameActivityInside() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ScheduleEditor.apply(BASE, Activity.work("상담", "10:00", "11:00")));
        assertEquals("동일한 근무활동입니다.", error.getMessage());
    }

    @Test
    @DisplayName("겹치는 구간이 있는 근무표는 받지 않는다")
    void rejectsOverlappingSchedule() {
        List<Activity> broken = List.of(
                Activity.work("상담", "09:00", "12:30"),
                Activity.rest("점심", "12:00", "13:00"));

        assertThrows(IllegalArgumentException.class,
                () -> ScheduleEditor.apply(broken, Activity.work("회의", "10:00", "11:00")));
    }

    static void assertSchedule(List<Activity> actual, String... expected) {
        List<String> actualText = new ArrayList<>();
        for (Activity activity : actual) {
            actualText.add(activity.toString());
        }
        assertEquals(List.of(expected), actualText);
    }
}
