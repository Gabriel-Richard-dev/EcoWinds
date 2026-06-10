package br.com.ecowinds.model;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "ac_schedules")
public class AcSchedule extends BaseEntity {

    public enum Action { ON, OFF }

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private Action action;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    public AcSchedule() {}

    public AcSchedule(DayOfWeek dayOfWeek, LocalTime time, Action action, Boolean enabled) {
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.action = action;
        this.enabled = enabled;
    }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AcSchedule that = (AcSchedule) o;
        return dayOfWeek == that.dayOfWeek
                && Objects.equals(time, that.time)
                && action == that.action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dayOfWeek, time, action);
    }
}
