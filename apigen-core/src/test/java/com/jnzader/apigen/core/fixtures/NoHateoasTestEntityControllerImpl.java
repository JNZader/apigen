package com.jnzader.apigen.core.fixtures;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller implementation WITHOUT HATEOAS for integration tests.
 */
@RestController
@RequestMapping("/test-entities-no-hateoas")
public class NoHateoasTestEntityControllerImpl extends BaseControllerImpl<TestEntity, TestEntityDTO, Long>
        implements TestEntityController {

    public NoHateoasTestEntityControllerImpl(
            TestEntityService service,
            TestEntityMapperAdapter mapper
    ) {
        super(service, mapper, null); // null resourceAssembler disables HATEOAS
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
