# Changelog

## 3.0.0
### Added
- [New feature] Activity Local Dispatch: Allows Cadence worker to dispatch activity tasks through local tunnel after ScheduleActivity decisions are made. This is a performance optimization to reduce activity scheduling efforts.
- Pass TaskListActivitiesPerSecond to activity worker and remove the limit.
- Add missing workflowtype and activitytype metric tags.
### Changed
- [Breaking changes] Refactoring in Worker initialization path:
  - Worker.Factory -> WorkerFactory
  - Worker.FactoryOptions -> WorkerFactoryOptions
  - PollerOptions.Builder -> PollerOptions.newBuilder
  - SingleWorkerOptions.Builder -> SingleWorkerOptions.newBuilder
  - Added WorkerOptions Builder
  - WorkflowClient.newInstance(IWorkflowService, Domain, WorkflowClientOptions) -> WorkflowClient.newInstance(IWorkflowService, WorkflowClientOptions)
  - WorkflowClientOptions.Builder -> WorkflowClientOptions.newBuilder
  - Testing framework
- Fix activity end-to-end latency metric.
- Fix newProxyInstance with the correct class.
- Fix bug in worker.isSuspended().
- Improve worker start/shutdown logic.
- Improve retry logic.
- Fix race condition during serialization.

## 2.7.8
- Fix get raw history 
- Improve signal processing error and log 
- Fix replay error when querying workflow that contains activity retry 

## 2.7.6
- Fix getVersion override when added new version 
- Add async signal to untypedstub 
- Fix RetryOptions.addDoNotRetry
- Add missing metrics from go client
- Fix a bug in setting retry expiration while getting history
- Fix start async return 

## 2.7.5
- Added supports contextPropagators for localActivity

## v2.7.4
- Fix prometheus reporting issue 
- Fix Promise.allOf should not block on empty input
- Misc: Added project directory to sourceItems path
- Add async start to untype stub

## v2.7.3
- Add wf type tag in decider metrics scope
- Fix WorkflowStub.fromTyped method
- Added missing fields to local activity task
- Honor user timeout for get workflow result

## v2.7.2
- Fix leak in Async GetWorkflowExecutionHistory
- Fix context timeout in execute workflow

## v2.7.1
- Fix a bug in build.gradle that prevented javadoc and sources from being published

## v2.7.0
- Add ParentClosePolicy to child workflows and also expose parent execution info for child workflows
- Add context propagation
- Fix various bugs around test workflow service and test mutable state implementation
- Use thrift IDLs from uber/cadence-idl repo as a submodule
- Various dependency updates including Docker base image and Gradle wrapper
- Miscellaneous bug fixes 

## v2.6.3
- Add Upsert Search Attributes
- Support get search attributes inside workflow

## v2.6.2
- Cleanup threads created in signal method on decider close
- Fixed exception propagation from child workflows

## v2.6.1
- Add missing metrics and tags
- Add metrics for SCHEDULED_TO_STAR latency
- Support filtering query based on close status
- Documentation improvements

## v2.6.0
- Fix thread leak on non-deterministic error
- Support Search Attributes on Start workflow
- Make Worker.addWorkflowImplementationFactory method support 'options'

## v2.5.2
- Add saga class that helps user to implement saga pattern in Cadence
- Add activity tasklist rate limiter option to worker options

## v2.5.1
- Fix busy loop in local activity poller if there is no task
- Fix an issue in get history timeout
- Lock decider while processing
- Timer firing fix

## v2.5.0
- Local activities
- Make sure signals are applied in the same order as they appear in history
- Fix retry option without expiration in test env
- Maintain correct runID during reset for random ID generation
- Default retry policy for Cadence service API calls
- Change markers to use headers to serialize internal fields
- Miscellaneous stability and debuggability fixes

## v2.4.2
- Support Memo in visibility
- Fix getVersion without decision event
- Make NoopScope metric scope really a no-op operation
- Add some more fields in activity info
- Wire workflow id reuse policy in start workflow execution params
- Add missing metrics tags for activity and decision task processing
- Multiple fixes to get build and unit-tests passing when building cadence-java-client from Windows.
- Allow data converter to handle non-serializable throwables in the cause chain

## v2.4.1
- Update default long poll timeout to 2min to match server side config
- Fix deadlock in sticky decider cache eviction
- Fix cron schedule merge issue in child workflow option

## v2.4.0
- Fixed InternalServiceError Error message on continue_as_new
- Correctly calculate workflow e2e latency
- Exposing CancellationScope.run method
- Add TBase and TEnum type adapter for JsonDataConverter
- Cron child workflow

## v2.3.1
- Added support for SignalWithStart Service API
- Expose methods in TChannel service to allow user to add headers in Thrift request

## v2.3.0
- Added cron schedule support.
- Fix infinite retryer in activity and workflow worker due to non-retryable error.
- Fixed hanging on testEnv.close when testEnv was not started.
- Fix for NPE when method has base type return type like int.
- Fixed JsonDataConverter to correctly report non serializable exceptions.

## v2.2.0
- Added support for workflow and activity server side retries.
- Clean worker shutdown. Replaced Worker shutdown(Duration) with Worker shutdown, shutdownNow and awaitTermination.
- Fixed thread exhaustion with a large number of parallel async activities.

## v2.1.3
- Added RPC headers needed to enable sticky queries. Before this change
queries did not used cached workflows.

## v2.1.2
- Requires minimum server release v0.4.0
- Introduced WorkerFactory and FactoryOptions
- Added sticky workflow execution, which is caching of a workflow object between decisions. It is enabled by default, 
to disable use FactoryOptions.disableStickyExecution property.
- Updated Thrift to expose new types of service exceptions: ServiceBusyError, DomainNotActiveError, LimitExceededError
- Added metric for corrupted signal as well as metrics related to caching and evictions.

## v1.0.0 (06-04-2018)
- POJO workflow, child workflow, activity execution.
- Sync and Async workflow execution.
- Query and Signal workflow execution.
- Test framework.
- Metrics and Logging support in client.
- Side effects, mutable side effects, random uuid and workflow getVersion support.
- Activity heartbeat throttling.
- Deterministic retry of failed operation.


