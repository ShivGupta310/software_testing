# Test Plan
This document outlines the planned testing strategy for a selected subset of requirements for the ILP REST API. This plan focuses on a representative set of high-impact requirements rather than exhaustive coverage. The goal is to demonstrate structured test planning, appropriate selection of test levels, and the use of scaffolding and instrumentation to support effective testing across the system lifecycle.

## Selected Requirements and Rationale for Selection
| Requirement                             | Type        | Level              | Rationale for Selection          |
|:----------------------------------------|:------------|:-------------------|----------------------------------|
| FR1-2: Order Completion Mechanism       | Functional  | System             | End-to-end delivery validity     |
| FR5: Drone Move Step Compliance         | Correctness | Unit/System        | Fundamental movement correctness |
| FR7: No-Fly Zone Compliance             |  Safety           | Integration/System | Regulatory constraint            |
| MQA2: API Response Latency Performance  | Performance            | System             | User-facing responsiveness       |

#### Rationale for Test Strategy
The requirements were deliberately selected to span:
* multiple requirements types (correctness, safety, performance)
* multiple levels of testing (unit, integration, system)
* multiple abstraction layers (mathematical primitives to full API behaviour)
* multiple risk domains (path invalidation, safety considerations, usability in deployment)


## Test Planning by Requirement

### FR1–2: Order Completion Mechanism
* **Requirement Summary:** The drone must hover at each delivery location, complete all assigned deliveries, and return to the service point after the final delivery.


* **Priority and Criticality:** Critical. Failure results in incomplete deliveries and invalid mission plans.


* **Testing Strategy:** Scenario-based black box testing would be ideal. Treat the API just as an external customer/consumer would. Representative delivery bundles are submitted and the resulting flight paths are analysed for required structural properties.


* **Test Level and Rationale:** System testing is required because hover behaviour and return-to-base logic are emergent properties of the full delivery planning pipeline with many dependencies on almost all the other services. This is the most effective way to test the desired behaviour especially since no single service or class can be meaningfully tested in isolation for this requirement. Testing at any of the lower levels would not provide evidence that the observable API behaviour meets the requirement.


* **Scaffolding and Instrumentation:** Predefined delivery requests with known service points and delivery locations are constructed to exercise single-order and multi-order completion paths. Returned flight paths are logged and inspected to verify duplicated coordinates at delivery points and at the final return to the service point.

### FR5: Drone Move Step Compliance
* **Requirement Summary:** Every drone movement must have a fixed magnitude (0.00015) and a direction constrained to multiples of 22.5 degrees.


* **Priority and Criticality:** High. Any violation produces an invalid flight path and breaks downstream assumptions.


* **Testing Strategy:** This requirement is tested using deterministic mathematical verification. Known inputs are used to generate movement vectors, which are checked against the specification using precise numeric assertions.


* **Test Level and Rationale:** Unit testing is used to validate the PositionService.nextPosition method in isolation, as step length and angle correctness are properties of individual movements. System testing is used to confirm that these constraints remain satisfied when movement logic is exercised indirectly via full delivery path generation.
  Unit tests isolate correctness of the movement primitive; system tests ensure that correct primitives are not misused or bypassed at higher levels.


* **Scaffolding and Instrumentation:** Synthetic coordinate pairs and angles are used to create predictable movement scenarios without requiring full delivery requests. Assertions record and validate step length and direction. On failure, diagnostic logging captures the computed values to support fault localisation.

### FR7: No-Fly Zone Compliance

* **Requirement Summary:** Generated flight paths must not enter predefined restricted regions.


* **Priority and Criticality:** Critical. Violations represent safety failures and breach regulatory assumptions.


* **Testing Strategy:** Constraint-based testing is used, analysing generated flight paths against known geometric region definitions. Boundary-adjacent cases are prioritised to maximise fault detection.


* **Test Level and Rationale:** Integration testing validates the interaction between path generation logic and region-checking utilities. It also ensures correct enforcement mechanisms; system testing ensures those mechanisms hold in full scenarios. System testing validates that complete delivery paths respect safety constraints under realistic API usage.


* **Scaffolding and Instrumentation:** Delivery scenarios are constructed such that the shortest path would intersect a no-fly zone if restrictions were not enforced.Assertions automatically check every generated position against no-fly zone polygons. On failure, the violating position and region are logged.



### MQA2: API Response Latency Performance 

* **Requirement Summary:** API endpoints for calculate flight paths should respond in under 5.5 seconds.


* **Priority and Criticality:** Medium. Performance degradation affects usability but not correctness.


* **Testing Strategy:** Black-box timing measurements are taken under controlled workloads to evaluate end-to-end API latency.


* **Test Level and Rationale:** System testing is required because response time is an emergent property of request parsing, validation, service orchestration, and response serialisation. Unit and integration tests cannot capture this behaviour meaningfully.


* **Scaffolding and Instrumentation:** Synthetic delivery requests of typical size are generated to ensure consistent and repeatable workloads.Timing instrumentation is added at the test harness level to record request–response durations. Results are logged across repeated runs to detect anomalies.


## Risk Analysis and Mitigation Strategy

The testing strategy for the ILP Drone Delivery API is influenced by several technical and process-related risks. Identifying these risks early allows testing effort to be prioritised toward areas where failures would have the highest impact or be hardest to diagnose later.

### R1: Geospatial Edge Case Risk
**Risk:** The system operates over a continuous geospatial domain. Small floating-point inaccuracies or boundary conditions may cause drones to incorrectly enter restricted no-fly zones or violate movement constraints without being detected by coarse tests.

**Impact:** High. Undetected boundary violations compromise safety guarantees and invalidate key functional requirements such as no-fly zone compliance and legal movement constraints.

**Mitigation:**
* Boundary value tests are explicitly designed for restricted-area edges and step-size thresholds
* Integration tests validate behaviour near no-fly zones rather than only obvious violations
* Instrumentation logs drone positions relative to restricted boundaries during test execution to assist in diagnosing subtle failures

### R2: Component Interaction Risk

**Risk:** Correct behaviour of the system depends on the interaction between multiple components (path planner, restricted-area logic, step validation). Components that behave correctly in isolation may still produce invalid results when combined.

**Impact:** High. Many safety and correctness properties only emerge at integration or system level.

**Mitigation:**
* Integration tests explicitly exercise planner–safety interactions
* System-level tests validate full request lifecycles via the REST API
* Tests are structured to fail loudly when invalid paths are produced, rather than silently correcting behaviour

### R3: False Confidence from Unit Tests
**Risk:** Over-reliance on unit tests may give a misleading sense of correctness, particularly for properties such as global path validity and delivery completion, which cannot be validated at unit level

**Impact:** Medium. Could result in undetected system-level defects.

**Mitigation:**
* Unit testing is deliberately limited to deterministic, local logic
* Higher-level tests are prioritised for requirements that span multiple components
* Test planning explicitly justifies why certain requirements cannot be adequately tested at unit level

### R4: Performance Measurement Distortion
**Risk:** Instrumentation used for performance testing (e.g. logging or timers) may itself alter system behaviour, producing misleading runtime measurements.

**Impact:** Medium. Performance requirements are advisory rather than deployment critical but still important metrics.

**Mitigation:**
* Instrumentation is kept lightweight and selectively enabled
* Runtime measurements are taken at system boundaries rather than deep internal loops
* Repeated test runs are used to smooth out transient measurement noise

## Expanded Scaffolding and Instrumentation Overview

### Scaffolding:
* **Synthetic Input Generators:** Predefined and programmatically generated delivery requests are used to exercise specific behaviours such as boundary navigation, multi-step paths, and no-fly zone avoidance.
* **Planner Invocation Harnesses:** Direct calls to planning components allow integration testing without HTTP or serialization overhead. Enables clearer fault localisation and faster feedback.
* **API Test Clients:** REST clients are used in system tests to simulate realistic usage patterns and validate request/response behaviour end-to-end.

Scaffolding is intentionally minimal and aligned with the existing codebase to avoid introducing artificial behaviour that would invalidate results.

### Instrumentation
Primary mechanisms would include:
* **Execution Timing:** Lightweight timers measure the duration of pathfinding operations for performance requirements. These measurements are taken at well-defined system boundaries to minimise distortion.
* **Path Validation Logging:** During integration and system tests, generated paths are logged or inspected to record: step count, movement vectors, boundary checks, restricted-area interactions.
* **Assertion-Based Safety Checks** Assertions embedded within test code automatically detect violations such as: entry into no-fly zones, invalid step magnitudes or directions, discontinuous paths. 

This approach ensures that failures are detected early and reported with sufficient contextual information for debugging.

