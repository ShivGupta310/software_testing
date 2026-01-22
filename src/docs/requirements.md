# Requirements Specification

## 1: Context
This document defines the requirements used as the basis for testing the ILP REST API.
The system is a simulated drone delivery route planning and orchestration API service. 
As this is purely a mathematical tool for geospatial route planning (no real world drones) the requirements are naturally centered around 
logical correctness, code security, performance, and optimality of route planning. 

For every requirement, a description, its impact on the code and external stakeholders, and a potential testing strategy is specified. 

## 2: Functional Requirements

### a: Correctness
**FR1-2: Order Completion Mechanism**
* **Description:** This encompasses two requirements. a) The delivery location coordinates (x, y) should appear twice in the flight path, signifying the drone "hovering" for a move to drop off the delivery. b) after completing all deliveries the drone must return back to the original service point, the trip from the final delivery location back to the service point should be included in the flight path response. The drone should hover over the service point on the return trip similar to a).  


* **Impact:** Critical. Failure to comply with the hover or the return will ultimately be an unsuccessful delivery and a loss of cargo or the drone or both. Impacts both business and consumer stakeholders. 


* **Test Approach/Level:** Delivery scenarios containing multiple valid orders are constructed and submitted to the API, and returned delivery plans are inspected.
  This requirement is tested at the system integration level via the /calcDeliveryPath endpoint, as order completion emerges only from the interaction of multiple services.

**FR3: Cost Heuristic**

* **Description:** To assign drones to a delivery(ies), the system makes needs an estimate of the move cost for a delivery to estimate which drones are capable of fulfilling the delivery.
The ILP coursework specification lacks any formal outline of how to do this, thus designing for it and defining the requirement is critical.

* The system should estimate the cost for a group of deliveries as: $$total Cost = 1/moveSize * (dist(sp, x_1) + dist(x_1, x_2) + dist(x_2, x_3)+... +dist(x_n, sp))$$


* **Impact:** Fundamental to the core function of the system. Errors or poor implementation may lead to the API assigning drones to too long deliveries, which the drones cannot undertake.


* **Test Approach/Level:** Unit testing of the functions used to calculate flight path candidates. 

**FR4: Capacity Enforcement**

* **Description:** Drones shall not be assigned deliveries that exceed their stated payload or operational constraints.


* **Impact:** High. Violating capacity constraints produces invalid delivery plans and breaks assumptions made by downstream consumers of the API.


* **Test Approach/Level:** Integration testing of /queryAvailableDrones and /calcDeliveryPath endpoints using constrained and unconstrained dispatch scenarios.

**FR5: Drone Move Step Compliance**

* **Description:** Every drone step in a flight plan must be a vector whose direction is a multiple of 22.5 degrees (with 0 degrees representing North) and whose magnitude is 0.00015 (the drone move step constant). 


* **Impact:** Critical. The generated flight paths need to comply with the legal directions the drones are allowed to move in or else they will result in failure. 


* **Test Approach/Level:** This requirement is tested at both the unit level (via geometric endpoints) and the system level (via flight delivery paths), as step compliance is enforced locally but observed globally.

**FR6: Multi-order Deliveries** 

* **Description:** The system shall support delivery plans involving multiple orders and multiple drones, producing a coherent combined delivery plan. The specific implementation is not of concern as long as it is consistent: tie-breakers defined, rules for ordering the bundle specified. 


* **Impact:** Critical. The API needs to be able to receive requests as a bundle of deliveries to be made, and respond with the flight path for a singular drone that can satisfy these requests. Without order bundling this will be inefficient as the drones waste range on frequent return trips to the service points. 


* **Test Approach/Level:** This requirement is tested at the system integration level, as bundling logic only emerges from combined routing and assignment behaviour.

### b: Security and Robustness
#### FR7: No-Fly Zone Compliance
* **Description:** The system should not generate any paths that include a drone flying through the restricted "no-fly zones".
Currently, the ILP Rest API provides 2 such zones: George Square and Bristo Square (both  quadrilateral regions).


* **Impact:** Critical impacts on the security of the drone. Influences legal compliance and government/aviation authorities. Strict compliance essential for any reliable simulation or future real-world system.


* **Test Approach/Level:** Unit and system testing. unit-level geometric checks (/isInRegion) and system-level inspection of /calcDeliveryPath outputs, reflecting both local and global enforcement.



**FR8: Rejection of Cargo When Unable to Deliver Safely**

* **Description:** The system shall reject or exclude delivery requests that cannot be safely fulfilled given drone constraints (heating, cooling, capacity, distance). 


* **Impact:** High. Prevents the generation of misleading or unsafe delivery plans. Ownership is on the API to reject delivery bundles that cannot be handled by a single drone. 


* **Test Approach/Level:** Delivery requests are constructed that violate drone constraints such as cooling capability, payload capacity, or total reachable distance, and system responses are examined to ensure such requests are rejected or excluded.
  This requirement is tested at the integration level via /queryAvailableDrones and /calcDeliveryPath, where feasibility decisions are made

**FR9: Non-Stochastic Algorithms**

* **Description:** The system is intended to be a route planner for a fictitious medical supply dispatch organisation. In such a scenario, route planning needs reproducability as a robustness feature. The system should under the same input conditions return the same path for the drones to follow. Stochastic changes would make the entire system less stable in the long term and make auditing it difficult.


* **Impact:** High, but not critical. Stochastic algorithms have their benefits, but for the test and dev teams working with a deterministic system (especially given the simulation) conditions would be ideal.


* **Test Approach/Level:** Identical delivery requests are submitted multiple times under identical conditions, and the returned delivery paths are compared for structural and numerical equality.
  This requirement is tested at the system level, as determinism must hold across the entire planning pipeline rather than within isolated services.

**FR10: Input Error Validation**

* **Description:** The API shall reject syntactically or semantically invalid input without exposing internal exceptions or stack traces.


* **Impact:** Medium to high. Protects system robustness and prevents undefined behaviour.


* **Test Approach/Level:**  Malformed JSON payloads, missing required fields, and semantically invalid values are submitted to API endpoints to verify controlled rejection without exposed exceptions.
  This requirement is tested at the controller level using MockMvc, where validation and HTTP error handling are observable.

## 3: Measurable Quality Attributes

**MQA1: Pathfinding Performance**

* **Description:** Delivery path calculation shall complete within a reasonable time for  typical input sizes defined by the coursework. This is defined as for any list of orders (5 or less) in the central Edinburgh area the local pathfinding service returns a response in under 2500 ms. Note: these statistics are assuming sufficient hardware: 12th generation processor and 16GB RAM.   


* **Impact:** Medium. Performance degradation reduces usability but does not invalidate correctness. System is ultimately simulated and not real time, so run-time measures are more for algorithmic and code complexity rather than deployment concerns.


* **Test Approach/Level:** Execution time for delivery path calculation is measured using wall-clock timing for representative request sizes under controlled local conditions.
  This attribute is evaluated at the system level, as performance characteristics depend on the complete routing pipeline.

**MQA2: API Response Latency Performance**

* **Description:**  API endpoints shall respond within a time frame suitable for interactive human use, not real time drone orchestration. For a hardware system running the client and server locally this is defined to be under 5500ms latency from request to response. The request should be of typical size (see previous MQA1).  


* **Impact:** Medium. See MQA1. 


* **Test Approach/Level:** End-to-end latency is measured from request submission to response receipt for representative API calls.
  This attribute is evaluated at the API integration level, without specialised load-testing tools.

**MQA3: Memory Performance**

* **Description:** The system shall not exhibit unbounded memory growth during  operation. Majority of allocated memory (75%) should be done at initialisation.


* **Impact:** Medium. Concerns over good system design for cloud API deployment where unbounded memory growth and significant growth after initialisation are costly mistakes. Assigning measured quality requirements helps check and reinforce good software design choices.


* **Test Approach/Level:** Memory usage is observed during repeated request execution to identify abnormal growth beyond initial allocation.
  This attribute is assessed qualitatively at the system level, acknowledging the absence of formal memory profiling.

## 4: Special and Qualitative Requirements

**SR1: Non-Traditional Response Codes**

* **Description:** Unlike a standard API which can use different response codes on failed/unsuccessful queries to help provide more insight into the reason for failure, this API should only return 400 as a failure code (not 40X variants).


* **Impact:** Critical. ILP specification for the auto-marker to work successfully. 


* **Test Approach/Level:** This requirement is tested at the controller integration level, where response codes are directly observable.

**SR2: Decoupled, Modular Code**

* **Description:** The code shoule be structured well, resources abstracted via services, distinct responsibilities for classes, use of DTOs and no circular dependencies between packages.


* **Impact:** Medium to High. Not essential for development, but will improve maintainability in the future.


* **Test Approach/Level:** The ability to test services independently using mocks and isolated contexts is evaluated through the construction of unit and service-level tests.
  This requirement is assessed indirectly at the design and unit-testing level, rather than through runtime assertions. Qualitative code inspections are also effective. 





