package kr.ac.tukorea.ue.domain.stat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class KpiDailyId implements Serializable {

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "university_id", nullable = false)
    private Long universityId;
}
