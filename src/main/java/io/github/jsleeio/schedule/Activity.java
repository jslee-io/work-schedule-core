package io.github.jsleeio.schedule;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

// 근무표 안의 활동 구간 하나. 종료 시각은 구간에 포함하지 않는다
public final class Activity {

    // 예제의 근무일. 자정을 넘긴 시각은 익일로 표기한다
    static final LocalDate WORK_DATE = LocalDate.of(2026, 1, 1);

    private final String name;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final boolean work;

    public Activity(String name, LocalDateTime start, LocalDateTime end, boolean work) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("활동 이름이 비어 있습니다.");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("시작 시각은 종료 시각보다 빨라야 합니다.");
        }
        this.name = name;
        this.start = start;
        this.end = end;
        this.work = work;
    }

    // 근무시간으로 계산하는 활동
    public static Activity work(String name, String start, String end) {
        return new Activity(name, toDateTime(start), toDateTime(end), true);
    }

    // 근무시간으로 계산하지 않는 활동
    public static Activity rest(String name, String start, String end) {
        return new Activity(name, toDateTime(start), toDateTime(end), false);
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public boolean isWork() {
        return work;
    }

    public long minutes() {
        return Duration.between(start, end).toMinutes();
    }

    // 경계만 맞닿는 경우는 겹치지 않는 것으로 본다
    public boolean overlaps(Activity other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public Activity withTime(LocalDateTime newStart, LocalDateTime newEnd) {
        return new Activity(name, newStart, newEnd, work);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Activity)) {
            return false;
        }
        Activity other = (Activity) o;
        return work == other.work
                && name.equals(other.name)
                && start.equals(other.start)
                && end.equals(other.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, start, end, work);
    }

    @Override
    public String toString() {
        return toText(start) + "-" + toText(end) + " " + name;
    }

    static LocalDateTime toDateTime(String text) {
        boolean nextDay = text.startsWith("익일 ");
        LocalTime time = LocalTime.parse(nextDay ? text.substring(3) : text);
        return LocalDateTime.of(nextDay ? WORK_DATE.plusDays(1) : WORK_DATE, time);
    }

    static String toText(LocalDateTime dateTime) {
        String time = dateTime.toLocalTime().toString();
        return dateTime.toLocalDate().isAfter(WORK_DATE) ? "익일 " + time : time;
    }
}
