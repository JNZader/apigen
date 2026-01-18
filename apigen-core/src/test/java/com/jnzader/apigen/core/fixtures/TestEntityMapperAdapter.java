package com.jnzader.apigen.core.fixtures;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter that wraps TestEntityMapper to implement BaseMapper.
 * This is needed because TestEntityDTO is a record (immutable) so we can't
 * implement updateDTOFromEntity directly.
 */
@Component
public class TestEntityMapperAdapter implements BaseMapper<TestEntity, TestEntityDTO> {

    private final TestEntityMapper delegate;

    public TestEntityMapperAdapter(TestEntityMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public TestEntityDTO toDTO(TestEntity entity) {
        return delegate.toDTO(entity);
    }

    @Override
    public TestEntity toEntity(TestEntityDTO dto) {
        return delegate.toEntity(dto);
    }

    @Override
    public List<TestEntityDTO> toDTOList(List<TestEntity> entities) {
        return delegate.toDTOList(entities);
    }

    @Override
    public List<TestEntity> toEntityList(List<TestEntityDTO> dtos) {
        return delegate.toEntityList(dtos);
    }

    @Override
    public void updateEntityFromDTO(TestEntityDTO dto, TestEntity entity) {
        delegate.updateEntityFromDTO(dto, entity);
    }

    @Override
    public void updateDTOFromEntity(TestEntity entity, TestEntityDTO dto) {
        // Records are immutable, so this is a no-op
        // In practice, you would create a new record instead of updating
    }
}
