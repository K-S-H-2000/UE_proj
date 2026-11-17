package kr.ac.tukorea.ue.domain.search.entity;

import jakarta.persistence.*;
import kr.ac.tukorea.ue.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "synonyms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_synonym", columnNames = {"keyword", "synonym"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Synonym extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "synonym_id")
    private Long id;

    @Column(name = "keyword", length = 50, nullable = false)
    private String keyword;

    @Column(name = "synonym", length = 50, nullable = false)
    private String synonym;

    @Builder
    public Synonym(String keyword, String synonym) {
        this.keyword = keyword;
        this.synonym = synonym;
    }
}
