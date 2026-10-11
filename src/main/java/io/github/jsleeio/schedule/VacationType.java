package io.github.jsleeio.schedule;

// 휴가 종류. 오전 휴가는 근무 시작부터, 오후 휴가는 근무 종료 쪽부터 차감한다
public enum VacationType {

    FULL_DAY(Direction.NONE),
    AM_HALF(Direction.FROM_START),
    PM_HALF(Direction.FROM_END),
    AM_QUARTER(Direction.FROM_START),
    PM_QUARTER(Direction.FROM_END);

    static final int QUARTER_MINUTES = 120;

    enum Direction { NONE, FROM_START, FROM_END }

    private final Direction direction;

    VacationType(Direction direction) {
        this.direction = direction;
    }

    Direction direction() {
        return direction;
    }

    // 반차는 소정근로시간의 절반이다. 철야조처럼 절반이 30분 단위로 떨어지지 않으면 오전은 내리고 오후는 올린다
    public int minutes(int contractMinutes) {
        if (contractMinutes <= 0) {
            throw new IllegalArgumentException("소정근로시간은 0보다 커야 합니다.");
        }
        double half = contractMinutes / 2.0;
        return switch (this) {
            case FULL_DAY -> contractMinutes;
            case AM_HALF -> (int) Math.floor(half / 30) * 30;
            case PM_HALF -> (int) Math.ceil(half / 30) * 30;
            case AM_QUARTER, PM_QUARTER -> QUARTER_MINUTES;
        };
    }
}
