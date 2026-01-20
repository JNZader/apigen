package contracts.errors

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return 404 for non-existent entity"
    description """
        GET /{id} for a non-existent entity should return 404 Not Found
        with RFC 7807 Problem Detail format.
    """

    request {
        method GET()
        url("/test-entities/999999")
        headers {
            accept(applicationJson())
        }
    }

    response {
        status NOT_FOUND()
        headers {
            // RFC 7807 specifies application/problem+json
            header 'Content-Type': 'application/problem+json'
        }
        bodyMatchers {
            jsonPath('$.status', byEquality())
        }
        body([
            "status": 404
        ])
    }
}
