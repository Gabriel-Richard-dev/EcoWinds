package br.com.ecowinds.model;

import br.com.ecowinds.model.enums.HolidayScope;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "holidays", uniqueConstraints = @UniqueConstraint(columnNames = {"holiday_date", "scope"}))
public class Holiday extends BaseEntity {

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private HolidayScope scope;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(length = 64)
    private String type;

    public Holiday() {}

    public Holiday(LocalDate date, String name, HolidayScope scope, String source, String type) {
        this.date = date;
        this.name = name;
        this.scope = scope;
        this.source = source;
        this.type = type;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public HolidayScope getScope() { return scope; }
    public void setScope(HolidayScope scope) { this.scope = scope; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
