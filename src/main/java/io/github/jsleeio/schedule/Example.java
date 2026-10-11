package io.github.jsleeio.schedule;

import java.util.List;

public final class Example {

    private Example() {
    }

    public static void main(String[] args) {
        List<Activity> schedule = List.of(Activity.work("상담", "09:00", "18:00"));
        print("기본 근무", schedule);

        schedule = ScheduleEditor.apply(schedule, Activity.rest("점심", "12:00", "13:00"));
        print("점심 추가", schedule);

        schedule = ScheduleEditor.apply(schedule, Activity.work("회의", "11:30", "12:30"));
        print("회의 추가", schedule);

        schedule = ScheduleEditor.apply(schedule, Activity.work("연장근무", "18:00", "19:00"));
        print("연장근무 추가", schedule);

        List<Activity> night = List.of(Activity.work("상담", "22:00", "익일 07:00"));
        print("야간 근무에 휴게 추가", ScheduleEditor.apply(night, Activity.rest("휴게", "익일 02:00", "익일 03:00")));

        List<Activity> base = List.of(
                Activity.work("상담", "09:00", "12:00"),
                Activity.rest("점심", "12:00", "13:00"),
                Activity.work("상담", "13:00", "18:00"));

        print("오전반차와 오후반반차", VacationPlanner.apply(
                base, 480, List.of(VacationType.AM_HALF, VacationType.PM_QUARTER)));

        // 합계가 소정근로시간과 같아 종일휴가 한 건이 된다
        print("오전반차와 오후반차", VacationPlanner.apply(
                base, 480, List.of(VacationType.AM_HALF, VacationType.PM_HALF)));
    }

    private static void print(String title, List<Activity> schedule) {
        System.out.println(title);
        for (Activity activity : schedule) {
            System.out.println("  " + activity);
        }
        System.out.println();
    }
}
