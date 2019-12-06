package org.openkilda.functionaltests.spec.toggles

import static org.openkilda.functionaltests.extension.tags.Tag.SMOKE
import static org.openkilda.functionaltests.extension.tags.Tag.VIRTUAL

import org.openkilda.functionaltests.HealthCheckSpecification
import org.openkilda.functionaltests.extension.tags.Tags
import org.openkilda.messaging.error.MessageError
import org.openkilda.messaging.model.system.FeatureTogglesDto

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import spock.lang.Narrative

@Narrative("""
Feature Toggles is a special lever that allows to turn on/off certain Kilda features. For example, we can disable
creation of new flows via Northbound API. This spec verifies that Feature Toggle restrictions are applied correctly.
""")
/*Note that the 'flowReroute' toggle is tested under AutoRerouteSpec#"Flow goes to 'Down' status when an intermediate
switch is disconnected and there is no ability to reroute".
BFD toggle is tested in BfdSpec*/
@Tags(SMOKE)
class FeatureTogglesV2Spec extends HealthCheckSpecification {
    @Tags(VIRTUAL)
    //TODO (andriidovhan) remove VIRTUAL tag and add new message when Issue 2797 is fixed
    // new error message in APIv2 "Could not create flow: Flow create feature is disabled"
    def "System forbids creating new flows when 'create_flow' toggle is set to false"() {
        given: "Existing flow"
        def flow = flowHelperV2.randomFlow(topology.activeSwitches[0], topology.activeSwitches[1])
        flowHelperV2.addFlow(flow)

        when: "Set create_flow toggle to false"
        northbound.toggleFeature(FeatureTogglesDto.builder().createFlowEnabled(false).build())

        and: "Try to create a new flow"
        northboundV2.addFlow(flowHelperV2.randomFlow(topology.activeSwitches[0], topology.activeSwitches[1]))

        then: "Error response is returned, explaining that feature toggle doesn't allow such operation"
        def e = thrown(HttpClientErrorException)
        e.statusCode == HttpStatus.FORBIDDEN
        def errorDetails = e.responseBodyAsString.to(MessageError)
        errorDetails.errorMessage == "Could not create flow"
        errorDetails.errorDescription == "Flow create feature is disabled"

        and: "Update of previously existing flow is still possible"
        flowHelperV2.updateFlow(flow.flowId, flow.tap { it.description = it.description + "updated" })

        and: "Delete of previously existing flow is still possible"
        flowHelperV2.deleteFlow(flow.flowId)

        and: "Cleanup: set create_flow toggle back to true"
        northbound.toggleFeature(FeatureTogglesDto.builder().createFlowEnabled(true).build())
    }

    def "System forbids updating flows when 'update_flow' toggle is set to false"() {
        given: "Existing flow"
        def flow = flowHelperV2.randomFlow(topology.activeSwitches[0], topology.activeSwitches[1])
        flowHelperV2.addFlow(flow)

        when: "Set update_flow toggle to false"
        northbound.toggleFeature(FeatureTogglesDto.builder().updateFlowEnabled(false).build())

        and: "Try to update the flow"
        northboundV2.updateFlow(flow.flowId, flow.tap { it.description = it.description + "updated" })

        then: "Error response is returned, explaining that feature toggle doesn't allow such operation"
        def e = thrown(HttpClientErrorException)
        e.statusCode == HttpStatus.FORBIDDEN
        def errorDetails = e.responseBodyAsString.to(MessageError)
        errorDetails.errorMessage == "Could not update flow"
        errorDetails.errorDescription == "Flow update feature is disabled"

        and: "Creating new flow is still possible"
        def newFlow = flowHelperV2.randomFlow(topology.activeSwitches[0], topology.activeSwitches[1])
        flowHelperV2.addFlow(newFlow)

        and: "Deleting of flows is still possible"
        [newFlow, flow].each { flowHelperV2.deleteFlow(it.flowId) }

        and: "Cleanup: set update_flow toggle back to true"
        northbound.toggleFeature(FeatureTogglesDto.builder().updateFlowEnabled(true).build())
    }

    def "System forbids deleting flows when 'delete_flow' toggle is set to false"() {
        given: "Existing flow"
        def flow = flowHelperV2.randomFlow(topology.activeSwitches[0], topology.activeSwitches[1])
        flowHelperV2.addFlow(flow)

        when: "Set delete_flow toggle to false"
        northbound.toggleFeature(FeatureTogglesDto.builder().deleteFlowEnabled(false).build())

        and: "Try to delete the flow"
        northboundV2.deleteFlow(flow.flowId)

        then: "Error response is returned, explaining that feature toggle doesn't allow such operation"
        def e = thrown(HttpClientErrorException)
        e.statusCode == HttpStatus.FORBIDDEN
        def errorDetails = e.responseBodyAsString.to(MessageError)
        errorDetails.errorMessage == "Could not delete flow"
        errorDetails.errorDescription == "Flow delete feature is disabled"

        and: "Creating new flow is still possible"
        def newFlow = flowHelperV2.randomFlow(topology.activeSwitches[0], topology.activeSwitches[1])
        flowHelperV2.addFlow(newFlow)

        and: "Updating of flow is still possible"
        flowHelperV2.updateFlow(flow.flowId, flow.tap { it.description = it.description + "updated" })

        when: "Set delete_flow toggle back to true"
        northbound.toggleFeature(FeatureTogglesDto.builder().deleteFlowEnabled(true).build())

        then: "Able to delete flows"
        [flow, newFlow].each { flowHelperV2.deleteFlow(it.flowId) }
    }
}