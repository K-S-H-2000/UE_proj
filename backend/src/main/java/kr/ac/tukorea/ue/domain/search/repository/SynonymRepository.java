package kr.ac.tukorea.ue.domain.search.repository;

import kr.ac.tukorea.ue.domain.search.entity.Synonym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SynonymRepository extends JpaRepository<Synonym, Long> {
    List<Synonym> findByKeyword(String keyword);
}
