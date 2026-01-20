package contracts.rest

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should return paginated list of entities"
    description """
        GET / endpoint should return a paginated list of entities
        with HATEOAS structure and pagination headers.
    """

    request {
        method GET()
        url("/test-entities") {
            queryParameters {
                parameter 'page': '0'
                parameter 'size': '10'
            }
        }
        headers {
            accept(applicationJson())
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        bodyMatchers {
            jsonPath('$._embedded.testEntityDTOList', byType {
                minOccurrence(1)
            })
            jsonPath('$._embedded.testEntityDTOList[0].id', byRegex('[0-9]+'))
            jsonPath('$._embedded.testEntityDTOList[0].name', byRegex('.+'))
            jsonPath('$.page.size', byRegex('[0-9]+'))
            jsonPath('$.page.totalElements', byRegex('[0-9]+'))
            jsonPath('$.page.number', byRegex('[0-9]+'))
        }
    }
}
