package com.jnzader.apigen.core.fixtures;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test controller implementation for unit tests. */
@RestController
@RequestMapping("/test-entities")
public class TestEntityControllerImpl extends BaseControllerImpl<TestEntity, TestEntityDTO, Long>
        implements TestEntityController {

    public TestEntityControllerImpl(
            TestEntityService service,
            TestEntityMapperAdapter mapper,
            TestEntityResourceAssembler resourceAssembler) {
        super(service, mapper, resourceAssembler);
    }

    @Override
    protected String getResourceName() {
        return "TestEntity";
    }

    @Override
    protected Class<TestEntity> getEntityClass() {
        return TestEntity.class;
    }
}
