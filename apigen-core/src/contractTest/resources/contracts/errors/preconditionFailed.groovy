package contracts.errors

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return 412 when If-Match does not match"
    description """
        PUT /{id} with an If-Match header that does not match the current ETag
        should return 412 Precondition Failed for optimistic concurrency control.
    """

    request {
        method PUT()
        url("/test-entities/1")
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header 'If-Match': '"stale-etag-value"'
        }
        body([
            "id": 1,
            "name": "Update Attempt",
            "estado": true
        ])
    }

    response {
        status PRECONDITION_FAILED()
        headers {
            // RFC 7807 specifies application/problem+json
            header 'Content-Type': 'application/problem+json'
        }
    }
}
