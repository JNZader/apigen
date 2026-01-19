package contracts.errors

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return 400 Bad Request for invalid input"
    description """
        POST / with invalid data should return 400 Bad Request
        with RFC 7807 Problem Detail format.
    """

    request {
        method POST()
        url("/test-entities")
        headers {
            contentType(applicationJson())
            accept(applicationJson())
        }
        body([
            "name": "",
            "estado": null
        ])
    }

    response {
        status BAD_REQUEST()
        headers {
            // RFC 7807 specifies application/problem+json
            header 'Content-Type': 'application/problem+json'
        }
    }
}
