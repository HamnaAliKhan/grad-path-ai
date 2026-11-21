package ai.gradpath.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * REPOSITORY PATTERN: shared CRUD (findById, findAll, save, delete, count ...) written once for every entity.
 * Each concrete repository only adds its own queries.
 *
 * @param <T> entity type (all tables use an integer primary key)
 */
@NoRepositoryBean
public interface BaseRepository<T> extends JpaRepository<T, Integer> {
}
