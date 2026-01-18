package com.jnzader.apigen.core.fixtures;

import com.jnzader.apigen.core.infrastructure.hateoas.BaseResourceAssembler;
import org.springframework.stereotype.Component;

/**
 * Test resource assembler for unit tests.
 * Note: Must use the implementation class (with @RequestMapping), not the interface.
 */
@Component
public class TestEntityResourceAssembler extends BaseResourceAssembler<TestEntityDTO, Long> {

    public TestEntityResourceAssembler() {
        super(TestEntityControllerImpl.class);
    }
}
