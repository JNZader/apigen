package contracts.rest

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return count in X-Total-Count header"
    description """
        HEAD / endpoint should return the total count of entities
        in the X-Total-Count response header.
    """

    request {
        method HEAD()
        url("/test-entities")
    }

    response {
        status OK()
        headers {
            header 'X-Total-Count': $(regex('[0-9]+'))
        }
    }
}
