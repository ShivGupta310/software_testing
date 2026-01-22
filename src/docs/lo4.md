# Test Evaluation and Effectiveness

The main concepts relied on in the testing strategy were namely:
* Equivalence Partitioning - Dividing inputs into classes that should behave similarly
* Boundary Value Analysis - Testing at the edges of valid/invalid boundaries
* State Transition Testing - Testing drone state changes during delivery
* Decision Table Testing - Testing combinations of conditions
* Mutation Testing Awareness - Tests designed to catch common mutations (arithmetic, relational, logical operators)
* Property-Based Testing Concepts - Testing invariants that should always hold
* Contract Testing - Verifying pre/post conditions
* Metamorphic Testing - If input changes in X way, output should change in Y way


## Gaps and Omissions 

| Area                                       | Gap/Omission                                            | Explanation and Impact                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|--------------------------------------------|---------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Functional Coverage (Safety & Correctness) | Non-exhaustive exploration of geometric flight paths    | Safety requirements (e.g. FR7 – No-Fly Zone Compliance) are tested using equivalence partitioning and boundary-focused scenarios with synthetic coordinates. While representative cases (inside, outside, near-boundary) are covered, the geometric search space is effectively continuous. Combinatorial interactions between waypoint sequences, turn angles, polygon shapes, and multiple restricted regions are not exhaustively explored. This leaves a residual risk that rare geometric edge cases (e.g. near-tangential boundary crossings or compound region interactions) remain undetected. |
| Combinatorial Interactions                 | Limited coverage of interacting constraints             | Although individual constraints such as restricted zones, delivery sequencing, and return-to-service behaviour are tested, their full combinatorial interaction is only partially covered. For example, scenarios involving multiple deliveries interacting with restricted regions during both outbound and return legs are tested representatively rather than exhaustively. This limitation is primarily due to combinatorial explosion and time constraints.                                                                                                                                       |
| Structural (White-Box) Coverage            | No enforced statement or branch coverage targets        | The test suite provides strong behavioural confidence via integration and system tests, but no formal statement, branch, or data-flow coverage metrics are enforced. As a result, it cannot be guaranteed that all executable paths have been exercised. In particular, defensive branches, error-handling logic, and rare failure modes may remain untested even though high-level functionality appears correct.                                                                                                                                                                                     |
| Algorithmic Completeness                   | Limited observability of internal pathfinding decisions | The flight path generator is validated primarily through black-box and integration testing using invariants (e.g. paths avoid no-fly zones, paths end at service point). While these properties ensure correctness, they do not guarantee that all internal decision branches of the pathfinding algorithm are exercised. This is a known oracle limitation: correctness can be asserted, but algorithmic optimality and completeness cannot be fully verified without an authoritative reference implementation.                                                                                      |
| Performance Testing                        | Deterministic workloads only                            | Performance tests use fixed synthetic workloads and limited concurrency levels, producing point estimates for response time and throughput. These tests demonstrate that performance thresholds are met under controlled conditions but do not provide statistically meaningful guarantees about worst-case behaviour, long-term variability, or performance under real-world deployment factors such as JVM warm-up, garbage collection pressure, or heterogeneous request mixes.                                                                                                                     |
| Fault-Based Testing                        | No systematic mutation testing                          | While the test suite is designed with mutation awareness (e.g. tests would fail under common arithmetic, relational, or logical operator faults), no automated mutation testing tool is used. Consequently, fault-detection effectiveness is inferred qualitatively rather than measured quantitatively. This introduces uncertainty regarding the test suite’s ability to detect subtle implementation faults.                                                                                                                                                                                        |
| Input Domain Coverage                      | Restricted range of numeric values                      | Input values for coordinates, delivery counts, and timing are deliberately constrained to realistic operational ranges. Extreme numeric values (e.g. maximum coordinate magnitudes, degenerate polygons, or pathological precision cases) are not exhaustively tested. This trade-off prioritises realistic scenarios but limits robustness guarantees under adversarial or malformed inputs.                                                                                                                                                                                                          |
| Concurrency and Stress                     | Limited concurrency scaling                             | Concurrent request testing is limited to a small number of simultaneous requests. While this demonstrates basic thread safety and stability, it does not explore higher concurrency levels or stress conditions that might expose race conditions, contention, or resource exhaustion issues.                                                                                                                                                                                                                                                                                                          |
| Test Oracle Limitations                    | Inability to assert optimality in pathfinding           | For path generation, there is no reliable oracle to determine whether a generated path is globally optimal under all constraints. Tests therefore focus on validity and safety properties rather than optimality. This is an accepted limitation inherent to the problem domain and is explicitly documented rather than ignored.                                                                                                                                                                                                                                                                      |



## Setting Target Levels
Due to the gaps and omissions fundamental to the selective testing methodology targets were chosen aggressively. 

| Metric/Target                                                                                                                                 | Justification                                                                                                                                                                                                 |
|-----------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| High branch coverage ≥ 80%                                                                                                                    | Ensures most logical branches, including edge-case defensive paths, are exercised. Critical for preventing missed error conditions in path planning and validation logic.                                     |
| High method coverage ≥ 70% across main services                                                                                               | AvailabilityService, DeliveryPathService, PositionService, FlightPathGenerator form the core API functionality. High method coverage ensures adequate coverage of API endpoints and associated business logic |
| 100% mutation score: Statistical Fault Injection (Mutation) Analysis                                                                          | Ensures test-suite sensititvity to arithmetic, logical and relational errors. Fault-injection based validation metric. Provides evidence that tests detect subtle faults.                                     |
| All critical pre/postconditions verified (100%)                                                                                               | Contract testing target ensures API maintains invariants, input/output consistency, and data integrity.                                                                                                       |
| Achieve consistent performance on time metrics for multi-delivery orders (all sub 5.5 seconds) and under 2 seconds for single delivery orders | Sets stricter performance benchmarks than the requirement. Explicitly mentions consistency over repeated trials and validates deployment ready performance                                                    | 


### Mutants Used for Evaluation 

The following 5 logical and arithmetical mutations were made across two highly tested services that were vital for the selected requirements: /services/FlightPathGenerator.java and /services/PositionService.java. They were tested to see if they were caught by the test suite.

**1: Deleted Hover in /services/FlightPathGenerator.java**

* Line 42 deleted to remove the hover on destination functionality "flightPath.add(toResponsePosition(deliveryPos));"


* Lead to 7 tests failing in /path/. Namely, the unit tests in FlightPathGenerator: delivery path ends with hover, each delivery has hover in multi delivery route, etc.

**2: Disable No-Fly Zone check in Pathfinding in /services/FlightPathGenerator.java**

* line 209 changed to return true no matter what (effectively disabling any checks on no-fly zones).


* caught by 4 tests in FR7 (No-Fly Zone compliance) test suite. 

**3: Skip Adding Return Path in /services/FlightPathGenerator.java**

* Deleted line 59. Return path is calculated but not added to the final return DTO.


* Caught by 5 tests in /system and 7 tests in path. Mainly caught by move step validation FR5 unit and system tests.

**4: Changed Boolean Comparator in /services/PositionService.java**

* Change line 73 ending from  && to ||.


* Mutant was caught by a total of 4 tests in path, primarily those testing for FR7.

**5: Changed Trig Function in /services/PositionService.java**

* Line 31 double newLng = start.getLng() + STEP_CONST * Math.cos(rad); changed to double newLng = start.getLng() + STEP_CONST * Math.tan(rad);


* Caught by 2 tests in path. 

---

Ultimately the test suite thus achieved a Mutation Score of 100% with 5/5 mutants killed.

## Performance Against Targets

| Metric                             | Target     | Achieved          | Evidence/Notes                                                                                                                                                                                                                                                                                                                                           |
|------------------------------------|------------|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Branch Coverage (critical modules) | \>= 80%    | 69%-100% (varies) | <li> Refer to /docs/services_coverage.png and doc/coverage.png </li> <li> AvailabilityService: 50% (many HTTP response failure and networking branches un-tested to prioritise key pathfinding logic) </li> <li> DeliveryPathService: 80% </li> <li> FlightPathGenerator: 69% (some auxiliary logic not targeted) </li> <li> PositionService: 100% </li> |
| Method Coverage                    | \>= 70%    | 75%-100%          | <li> Refer to /docs/services_coverage.png and doc/coverage.png <li> AvailabilityService: 88% <li> DeliveryPathService: 100% <li> FlightPathGenerator: 75% <li> PositionService: 100%  </li>                                                                                                                                                              |
| Mutation Testing (5 mutants)       | 100%       | 100%              | All mutants killed (refer above). Mutants spanned arithmetic, logical, boolean fault-injections over the two most vital services for path generation. All detected.                                                                                                                                                                                      |
| Contract verification              | 100%       | Partial           | Pre/postconditions enforced in unit tests for AvailabilityService and DeliveryPathService. FlightPathGenerator and PositionService not yet fully contract-verified.                                                                                                                                                                                      |
| Response Time                      | <2s, <5.5s | consistent <200ms | <li> Refer to /docs/system_test_results.html <li> API response latency consistently between 160ms-170ms <li> Limited deviations across single delivery, multi delivery and concurrent requests <li> However testing limitations exist in this domain and should be kept inmind                                                                           |

### Comments:
Overall, the test suite meets or exceeds targets for method coverage, response latency, critical branch coverage, and fault detection in core services. Branch coverage for auxiliary logic and network error handling is below target. Contract verification and worst-case performance under concurrency remain areas of concern. There is scope for improved line coverage. Network test results appear suspicious and could warrant further investigation/a reconsidered test plan. 

## Improvements

| Improvement Area              | Proposed Actions                                                                                                                                                                     | Expected Outcome                                                                                          |
|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Branch coverage gaps          | Expand API test cases to include network failures, invalid requests, and edge-case path geometries; introduce targeted system-level tests for FlightPathGenerator auxiliary methods. | Achieve ≥ 80% branch coverage across all critical modules. Reduce risk of untested defensive code.        |
| Contract testing for mocks    | Implement Pact or equivalent to enforce API pre/postconditions between mocks and real service endpoints.                                                                             | Ensures unit tests remain valid despite underlying entity changes. Closes verification gap.               | 
| Performance under concurrency | 
| Automated mutation testing    | Integrate a mutation tool (e.g., PIT or MutPy) to systematically seed logic, arithmetic, relational, etc mutations.                                                                  | Quantitative evidence of test suite sensitivity; ensures detection of subtle faults not covered manually. |                                                                                                    | 
| Expanded input coverage       | Use combinatorial and randomised input generators for multi-leg deliveries, restricted zones, and edge-case coordinates.                                                             | Increased confidence in correctness across all geometric and combinatorial edge cases.                    |