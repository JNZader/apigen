package com.jnzader.apigen.core.fixtures;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import org.springframework.stereotype.Repository;

/** Test repository for unit tests. */
@Repository
public interface TestEntityRepository extends BaseRepository<TestEntity, Long> {}
