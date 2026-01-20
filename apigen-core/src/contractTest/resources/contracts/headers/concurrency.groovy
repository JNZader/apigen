package contracts.headers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return 412 when If-Match header does not match"
    description """
        PUT /{id} with non-matching If-Match header should return 412 Precondition Failed.
        This demonstrates optimistic concurrency control.
    """

    request {
        method PUT()
        url("/test-entities/1")
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header 'If-Match': '"non-matching-etag"'
        }
        body([
            "id": 1,
            "name": "Concurrent Update Entity",
            "estado": true
        ])
    }

    response {
        status PRECONDITION_FAILED()
        headers {
            header 'Content-Type': 'application/problem+json'
        }
    }
}
