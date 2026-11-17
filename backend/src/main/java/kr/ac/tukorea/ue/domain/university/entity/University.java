package kr.ac.tukorea.ue.domain.university.entity;

import jakarta.persistence.*;
import kr.ac.tukorea.ue.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "universities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class University extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "university_id")
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "email_domain", length = 100, nullable = false, unique = true)
    private String emailDomain;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    public University(String name, String emailDomain, Boolean isActive) {
        this.name = name;
        this.emailDomain = emailDomain;
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void updateStatus(boolean isActive) {
        this.isActive = isActive;
    }
}
