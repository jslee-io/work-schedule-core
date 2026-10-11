package io.github.jsleeio.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// 근무표에 활동을 넣고 겹치는 기존 구간을 정리한다
public final class ScheduleEditor {

    private ScheduleEditor() {
    }

    public static List<Activity> apply(List<Activity> current, Activity incoming) {
        List<Activity> sorted = sortAndValidate(current);

        // 원래 활동과 같은 활동을 그 자리에 다시 넣는 것은 막는다
        for (Activity existing : sorted) {
            boolean sameName = existing.getName().equals(incoming.getName());
            boolean covers = !existing.getStart().isAfter(incoming.getStart())
                    && !existing.getEnd().isBefore(incoming.getEnd());
            if (sameName && covers) {
                throw new IllegalArgumentException("동일한 근무활동입니다.");
            }
        }

        // 겹치는 구간은 신규 활동 앞뒤로 남는 시간만 원래 활동으로 남기고, 완전히 덮이면 없앤다
        // 빈 시간이나 기본 근무 밖에 넣은 활동은 겹치는 구간이 없으므로 그대로 신규 구간이 된다
        List<Activity> result = new ArrayList<Activity>();
        for (Activity existing : sorted) {
            if (!existing.overlaps(incoming)) {
                result.add(existing);
                continue;
            }
            if (existing.getStart().isBefore(incoming.getStart())) {
                result.add(existing.withTime(existing.getStart(), incoming.getStart()));
            }
            if (existing.getEnd().isAfter(incoming.getEnd())) {
                result.add(existing.withTime(incoming.getEnd(), existing.getEnd()));
            }
        }
        result.add(incoming);

        // 자정을 넘는 근무도 순서가 뒤집히지 않도록 날짜를 포함한 일시로 정렬한다
        Collections.sort(result, Comparator.comparing(Activity::getStart));
        return Collections.unmodifiableList(result);
    }

    static List<Activity> sortAndValidate(List<Activity> schedule) {
        List<Activity> sorted = new ArrayList<Activity>(schedule);
        Collections.sort(sorted, Comparator.comparing(Activity::getStart));
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i - 1).overlaps(sorted.get(i))) {
                throw new IllegalArgumentException("근무표 안에 겹치는 구간이 있습니다.");
            }
        }
        return sorted;
    }
}
