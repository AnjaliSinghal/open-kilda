package org.openkilda.functionaltests.spec.flows

import static org.openkilda.functionaltests.extension.tags.Tag.SMOKE
import static org.openkilda.functionaltests.helpers.thread.FlowHistoryConstants.CREATE_ACTION
import static org.openkilda.functionaltests.helpers.thread.FlowHistoryConstants.CREATE_SUCCESS
import static org.openkilda.functionaltests.helpers.thread.FlowHistoryConstants.UPDATE_SUCCESS
import static org.openkilda.functionaltests.helpers.thread.FlowHistoryConstants.UPDATE_ACTION
import static org.openkilda.testing.Constants.NON_EXISTENT_FLOW_ID

import org.openkilda.functionaltests.HealthCheckSpecification
import org.openkilda.functionaltests.extension.failfast.Tidy
import org.openkilda.functionaltests.extension.tags.Tags
import org.openkilda.messaging.payload.history.FlowEventPayload
import org.openkilda.testing.model.topology.TopologyDefinition.Switch

import spock.lang.Narrative
import spock.lang.Shared

import java.util.concurrent.TimeUnit

@Narrative("""Verify that history records are created for the create/update actions.
History record is created in case the create/update action is completed successfully.""")
class FlowHistoryV2Spec extends HealthCheckSpecification {
    @Shared
    Long timestampBefore

    def setupOnce() {
        timestampBefore = System.currentTimeSeconds() - 5
    }

    @Tidy
    def "History records are created for the create/update actions using custom timeline"() {
        when: "Create a flow"
        def (Switch srcSwitch, Switch dstSwitch) = topology.activeSwitches
        def flow = flowHelperV2.randomFlow(srcSwitch, dstSwitch)
        flowHelperV2.addFlow(flow)

        then: "History record is created"
        Long timestampAfterCreate = System.currentTimeSeconds()
        def flowHistory = northbound.getFlowHistory(flow.flowId, timestampBefore, timestampAfterCreate)
        assert flowHistory.size() == 1
        checkHistoryCreateV2Action(flowHistory[0], flow.flowId)

        and: "Flow history contains all flow properties in the dump section"
        with(flowHistory[0].dumps[0]) { dump ->
            dump.type == "stateAfter"
            dump.bandwidth == flow.maximumBandwidth
            dump.ignoreBandwidth == flow.ignoreBandwidth
            dump.forwardCookie > 0
            dump.reverseCookie > 0
            dump.sourceSwitch == flow.source.switchId.toString()
            dump.destinationSwitch == flow.destination.switchId.toString()
            dump.sourcePort == flow.source.portNumber
            dump.destinationPort == flow.destination.portNumber
            dump.sourceVlan == flow.source.vlanId
            dump.destinationVlan == flow.destination.vlanId
            dump.forwardMeterId > 0
            dump.forwardStatus == "IN_PROGRESS" // issue 3038
            dump.reverseStatus == "IN_PROGRESS"
            dump.reverseMeterId > 0
            //dump.allocateProtectedPath == flow.allocateProtectedPath // issue 3031
            //dump.encapsulationType == flow.encapsulationType
            //dump.pinned == flow.pinned
            //dump.pathComputationStrategy == flow.pathComputationStrategy
            //dump.periodicPings == flow.periodicPings
        }

        when: "Update the created flow"
        flowHelperV2.updateFlow(flow.flowId, flow.tap { it.description = it.description + "updated" })

        then: "History record is created after updating the flow"
        Long timestampAfterUpdate = System.currentTimeSeconds()
        def flowHistory1 = northbound.getFlowHistory(flow.flowId, timestampBefore, timestampAfterUpdate)
        assert flowHistory1.size() == 2
        checkHistoryUpdateAction(flowHistory1[1], flow.flowId)

        while((System.currentTimeSeconds() - timestampAfterUpdate) < 1) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        when: "Delete the updated flow"
        def deleteResponse = flowHelperV2.deleteFlow(flow.flowId)

        then: "History is still available for the deleted flow"
        def flowHistory3 = northbound.getFlowHistory(flow.flowId, timestampBefore, timestampAfterUpdate)
        assert flowHistory3.size() == 2
        checkHistoryDeleteAction(flowHistory3, flow.flowId)

        cleanup:
        !deleteResponse && flowHelperV2.deleteFlow(flow.flowId)
    }

    @Tidy
    @Tags(SMOKE)
    def "History records are created for the create/update actions using custom timeline (v2)"() {
        when: "Create a flow"
        def (Switch srcSwitch, Switch dstSwitch) = topology.activeSwitches
        def flow = flowHelperV2.randomFlow(srcSwitch, dstSwitch)
        flowHelperV2.addFlow(flow)

        then: "History record is created"
        Long timestampAfterCreate = System.currentTimeSeconds()
        verifyAll(northbound.getFlowHistory(flow.flowId, timestampBefore, timestampAfterCreate)) { flowH ->
            flowH.size() == 1
            checkHistoryCreateV2Action(flowH[0], flow.flowId)
        }

        when: "Update the created flow"
         def flowInfo = northboundV2.getFlow(flow.flowId)
        flowHelperV2.updateFlow(flowInfo.flowId,
                flowHelperV2.toRequest(flowInfo.tap { it.description = it.description + "updated" }))

        then: "History record is created after updating the flow"
        Long timestampAfterUpdate = System.currentTimeSeconds()
        verifyAll(northbound.getFlowHistory(flow.flowId, timestampBefore, timestampAfterUpdate)){ flowH ->
            flowH.size() == 2
            checkHistoryUpdateAction(flowH[1], flow.flowId)
        }

        while((System.currentTimeSeconds() - timestampAfterUpdate) < 1) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        when: "Delete the updated flow"
        def deleteResponse = flowHelperV2.deleteFlow(flow.flowId)

        then: "History is still available for the deleted flow"
        northbound.getFlowHistory(flow.flowId, timestampBefore, timestampAfterUpdate).size() == 2

        cleanup:
        !deleteResponse && flowHelperV2.deleteFlow(flow.flowId)
    }

    @Tidy
    def "History records are created for the create/update actions using default timeline"() {
        when: "Create a flow"
        def (Switch srcSwitch, Switch dstSwitch) = topology.activeSwitches
        def flow = flowHelperV2.randomFlow(srcSwitch, dstSwitch)
        flowHelperV2.addFlow(flow)

        then: "History record is created"
        def flowHistory = northbound.getFlowHistory(flow.flowId)
        assert flowHistory.size() == 1
        checkHistoryCreateV2Action(flowHistory[0], flow.flowId)

        when: "Update the created flow"
        flowHelperV2.updateFlow(flow.flowId, flow.tap { it.description = it.description + "updated" })

        then: "History record is created after updating the flow"
        def flowHistory1 = northbound.getFlowHistory(flow.flowId)
        assert flowHistory1.size() == 2
        checkHistoryUpdateAction(flowHistory1[1], flow.flowId)

        when: "Delete the updated flow"
        def deleteResponse = flowHelperV2.deleteFlow(flow.flowId)

        then: "History is still available for the deleted flow"
        def flowHistory3 = northbound.getFlowHistory(flow.flowId)
        assert flowHistory3.size() == 3
        checkHistoryDeleteAction(flowHistory3, flow.flowId)

        cleanup:
        !deleteResponse && flowHelperV2.deleteFlow(flow.flowId)
    }

    @Tidy
    def "History should not be returned in case timeline is incorrect (timeBefore > timeAfter)"() {
        when: "Create a flow"
        def (Switch srcSwitch, Switch dstSwitch) = topology.activeSwitches
        def flow = flowHelperV2.randomFlow(srcSwitch, dstSwitch)
        flowHelperV2.addFlow(flow)

        then: "History record is created"
        Long timestampAfterCreate = System.currentTimeSeconds()
        def flowHistory = northbound.getFlowHistory(flow.flowId, timestampBefore, timestampAfterCreate)
        assert flowHistory.size() == 1
        checkHistoryCreateV2Action(flowHistory[0], flow.flowId)

        when: "Try to get history for incorrect timeline"
        def flowH = northbound.getFlowHistory(flow.flowId, timestampAfterCreate, timestampBefore)

        then: "History record is NOT returned"
        flowH.isEmpty()

        cleanup: "Restore default state(remove created flow)"
        flowHelperV2.deleteFlow(flow.flowId)
    }

    @Tidy
    def "History should not be returned in case flow was never created"() {
        when: "Try to get history for incorrect flowId"
        def flowHistory = northbound.getFlowHistory(NON_EXISTENT_FLOW_ID)

        then: "History record is NOT returned"
        flowHistory.isEmpty()
    }

    void checkHistoryCreateV2Action(FlowEventPayload flowHistory, String flowId) {
        assert flowHistory.action == CREATE_ACTION
        assert flowHistory.histories.action[-1] == CREATE_SUCCESS
        checkHistoryCommonStuff(flowHistory, flowId)
    }

    void checkHistoryUpdateAction(FlowEventPayload flowHistory, String flowId) {
        assert flowHistory.action == UPDATE_ACTION
        assert flowHistory.histories.action[-1] == UPDATE_SUCCESS
        checkHistoryCommonStuff(flowHistory, flowId)
    }

    void checkHistoryCommonStuff(FlowEventPayload flowHistory, String flowId) {
        assert flowHistory.flowId == flowId
        assert flowHistory.taskId
    }

    /** We pass latest timestamp when changes were done.
     * Just for getting all records from history */
    void checkHistoryDeleteAction(List<FlowEventPayload> flowHistory, String flowId) {
        checkHistoryCreateV2Action(flowHistory[0], flowId)
        checkHistoryUpdateAction(flowHistory[1], flowId)
    }
}
