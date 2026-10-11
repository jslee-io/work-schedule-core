package io.github.jsleeio.schedule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 근무표에 휴가를 반영한다. 구간을 나누는 일은 구간 재구성 로직에 맡긴다
public final class VacationPlanner {

    public static final String VACATION = "휴가";

    private VacationPlanner() {
    }

    public static List<Activity> apply(List<Activity> schedule, int contractMinutes, List<VacationType> requests) {
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("반영할 휴가가 없습니다.");
        }
        List<Activity> sorted = ScheduleEditor.sortAndValidate(schedule);
        if (sorted.isEmpty()) {
            throw new IllegalArgumentException("근무 구간이 없는 근무표에는 휴가를 반영할 수 없습니다.");
        }

        // 휴가가 시간상 떨어져 있어도 하루 합계로 종일 여부를 판단한다
        int total = 0;
        for (VacationType request : requests) {
            total += request.minutes(contractMinutes);
        }
        if (total >= contractMinutes) {
            LocalDateTime dayStart = sorted.get(0).getStart();
            LocalDateTime dayEnd = sorted.get(sorted.size() - 1).getEnd();
            return Collections.singletonList(new Activity(VACATION, dayStart, dayEnd, false));
        }

        List<Activity> result = sorted;
        for (VacationType request : requests) {
            int remaining = request.minutes(contractMinutes);

            // 점심 같은 휴게 구간은 근무시간이 아니므로 차감 대상에서 뺀다
            List<Activity> workActivities = new ArrayList<Activity>();
            for (Activity activity : result) {
                if (activity.isWork()) {
                    workActivities.add(activity);
                }
            }
            boolean fromEnd = request.direction() == VacationType.Direction.FROM_END;
            if (fromEnd) {
                Collections.reverse(workActivities);
            }

            List<Activity> vacations = new ArrayList<Activity>();
            for (Activity activity : workActivities) {
                if (remaining == 0) {
                    break;
                }
                long take = Math.min(remaining, activity.minutes());
                LocalDateTime start = fromEnd ? activity.getEnd().minusMinutes(take) : activity.getStart();
                LocalDateTime end = fromEnd ? activity.getEnd() : activity.getStart().plusMinutes(take);
                vacations.add(new Activity(VACATION, start, end, false));
                remaining -= (int) take;
            }
            if (remaining > 0) {
                throw new IllegalArgumentException("휴가를 반영할 근무시간이 부족합니다.");
            }

            for (Activity vacation : vacations) {
                result = ScheduleEditor.apply(result, vacation);
            }
        }
        return result;
    }
}
