package io.github.jsleeio.schedule;

import static io.github.jsleeio.schedule.ScheduleEditorTest.assertSchedule;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VacationPlannerTest {

    // 소정근로 8시간 근무표
    private static final List<Activity> EIGHT_HOURS = List.of(
            Activity.work("상담", "09:00", "12:00"),
            Activity.rest("점심", "12:00", "13:00"),
            Activity.work("상담", "13:00", "18:00"));

    // 소정근로 10시간 30분 철야조 근무표
    private static final List<Activity> OVERNIGHT = List.of(
            Activity.work("상담", "20:00", "익일 02:00"),
            Activity.rest("휴게", "익일 02:00", "익일 03:00"),
            Activity.work("상담", "익일 03:00", "익일 07:30"));

    @Test
    @DisplayName("오전반차는 근무 시작부터 차감하고 점심은 건너뛴다")
    void morningHalfSkipsLunch() {
        List<Activity> result = VacationPlanner.apply(EIGHT_HOURS, 480, List.of(VacationType.AM_HALF));

        assertSchedule(result,
                "09:00-12:00 휴가",
                "12:00-13:00 점심",
                "13:00-14:00 휴가",
                "14:00-18:00 상담");
    }

    @Test
    @DisplayName("오후반반차는 근무 종료 쪽에서 2시간을 차감한다")
    void afternoonQuarterFromEnd() {
        List<Activity> result = VacationPlanner.apply(EIGHT_HOURS, 480, List.of(VacationType.PM_QUARTER));

        assertSchedule(result,
                "09:00-12:00 상담",
                "12:00-13:00 점심",
                "13:00-16:00 상담",
                "16:00-18:00 휴가");
    }

    @Test
    @DisplayName("오전반차와 오후반반차가 함께 오면 사이의 근무는 남는다")
    void morningHalfAndAfternoonQuarterKeepMiddleWork() {
        List<Activity> result = VacationPlanner.apply(
                EIGHT_HOURS, 480, List.of(VacationType.AM_HALF, VacationType.PM_QUARTER));

        assertSchedule(result,
                "09:00-12:00 휴가",
                "12:00-13:00 점심",
                "13:00-14:00 휴가",
                "14:00-16:00 상담",
                "16:00-18:00 휴가");
    }

    @Test
    @DisplayName("오전반차와 오후반차는 시간이 이어지지 않아도 합계로 종일휴가가 된다")
    void twoHalvesBecomeFullDay() {
        List<Activity> result = VacationPlanner.apply(
                EIGHT_HOURS, 480, List.of(VacationType.AM_HALF, VacationType.PM_HALF));

        assertSchedule(result, "09:00-18:00 휴가");
    }

    @Test
    @DisplayName("종일휴가는 근무 시작부터 종료까지 휴가 한 건이 된다")
    void fullDayIsSingleVacation() {
        List<Activity> result = VacationPlanner.apply(EIGHT_HOURS, 480, List.of(VacationType.FULL_DAY));

        assertSchedule(result, "09:00-18:00 휴가");
    }

    @Test
    @DisplayName("반차 시간은 주간 8시간이면 4시간, 철야조 10.5시간이면 오전 5시간과 오후 5.5시간이다")
    void halfDayMinutesFollowContractHours() {
        assertEquals(240, VacationType.AM_HALF.minutes(480));
        assertEquals(240, VacationType.PM_HALF.minutes(480));
        assertEquals(300, VacationType.AM_HALF.minutes(630));
        assertEquals(330, VacationType.PM_HALF.minutes(630));
        assertEquals(120, VacationType.AM_QUARTER.minutes(630));
    }

    @Test
    @DisplayName("10.5시간 철야조의 오전반차는 근무 시작부터 5시간을 차감한다")
    void overnightMorningHalf() {
        List<Activity> result = VacationPlanner.apply(OVERNIGHT, 630, List.of(VacationType.AM_HALF));

        assertSchedule(result,
                "20:00-익일 01:00 휴가",
                "익일 01:00-익일 02:00 상담",
                "익일 02:00-익일 03:00 휴게",
                "익일 03:00-익일 07:30 상담");
    }

    @Test
    @DisplayName("10.5시간 철야조의 오후반차는 근무 종료 쪽에서 5.5시간을 차감한다")
    void overnightAfternoonHalf() {
        List<Activity> result = VacationPlanner.apply(OVERNIGHT, 630, List.of(VacationType.PM_HALF));

        assertSchedule(result,
                "20:00-익일 01:00 상담",
                "익일 01:00-익일 02:00 휴가",
                "익일 02:00-익일 03:00 휴게",
                "익일 03:00-익일 07:30 휴가");
    }

    @Test
    @DisplayName("휴가 목록이 비었거나 소정근로시간이 0 이하면 막는다")
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> VacationPlanner.apply(EIGHT_HOURS, 480, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> VacationPlanner.apply(EIGHT_HOURS, 0, List.of(VacationType.AM_HALF)));
    }

    @Test
    @DisplayName("차감할 근무시간이 부족하면 막는다")
    void rejectsWhenNotEnoughWork() {
        List<Activity> shortDay = List.of(
                Activity.work("상담", "09:00", "10:00"),
                Activity.rest("점심", "10:00", "13:00"));

        // 소정근로 8시간 기준 오전반차 4시간을 1시간짜리 근무에서 뺄 수 없다
        assertThrows(IllegalArgumentException.class,
                () -> VacationPlanner.apply(shortDay, 480, List.of(VacationType.AM_HALF)));
    }
}
