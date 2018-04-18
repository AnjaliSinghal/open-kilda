package org.openkilda.integration.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.json.simple.JSONObject;
import org.openkilda.helper.RestClientManager;
import org.openkilda.integration.converter.IslLinkConverter;
import org.openkilda.integration.converter.PortConverter;
import org.openkilda.integration.exception.ContentNotFoundException;
import org.openkilda.integration.exception.IntegrationException;
import org.openkilda.integration.model.response.IslLink;
import org.openkilda.model.IslLinkInfo;
import org.openkilda.model.LinkProps;
import org.openkilda.model.PortInfo;
import org.openkilda.model.SwitchInfo;
import org.openkilda.service.ApplicationService;
import org.openkilda.utility.ApplicationProperties;
import org.openkilda.utility.CollectionUtil;
import org.openkilda.utility.IoUtil;
import org.openkilda.utility.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The Class SwitchIntegrationService.
 *
 * @author Gaurav Chugh
 */
@Service
public class SwitchIntegrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchIntegrationService.class);

    @Autowired
    private RestClientManager restClientManager;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private IslLinkConverter islLinkConverter;

    /**
     * Gets the switches.
     *
     * @return the switches
     * @throws IntegrationException
     */
    public List<SwitchInfo> getSwitches() {
        HttpResponse response = restClientManager.invoke(applicationProperties.getSwitches(),
                HttpMethod.GET, "", "", applicationService.getAuthHeader());
        if (RestClientManager.isValidResponse(response)) {
            List<SwitchInfo> switchesResponse =
                    restClientManager.getResponseList(response, SwitchInfo.class);
            return getSwitchInfoSetName(switchesResponse);
        }
        return null;
    }

    /**
     * Gets the SwitchInfoSetName.
     *
     * @return the switches
     * @throws IntegrationException
     */
    private List<SwitchInfo> getSwitchInfoSetName(List<SwitchInfo> switches) {

        LOGGER.info("Inside getSwitchInfoSetName : Start");
        if (switches != null && !StringUtils.isEmpty(switches)) {

            Map<String, String> csNames = getCustomSwitchNameFromFile();

            for (SwitchInfo switchInfo : switches) {
                switchInfo.setName(customSwitchName(csNames, switchInfo.getSwitchId()));
            }
        }
        return switches;
    }

    /**
     * This Method is used to set custom Switch name.
     * 
     * @param csNames
     * @param switchId
     * @return switch name
     */
    public String customSwitchName(Map<String, String> csNames, String switchId) {
        if (csNames != null && !StringUtils.isEmpty(csNames) && csNames.size() > 0) {
            if (csNames.containsKey(switchId.toLowerCase())
                    || csNames.containsKey(switchId.toUpperCase())) {
                if (!IoUtil.chkStringIsNotEmpty(csNames.get(switchId))) {
                    return switchId;
                } else {
                    return csNames.get(switchId);
                }
            } else {
                return switchId;
            }
        } else
            return switchId;
    }

    /**
     * Gets the isl links.
     *
     * @return the isl links
     */
    public List<IslLinkInfo> getIslLinks() {
        HttpResponse response = restClientManager.invoke(applicationProperties.getLinks(),
                HttpMethod.GET, "", "", applicationService.getAuthHeader());
        if (RestClientManager.isValidResponse(response)) {
            List<IslLink> links = restClientManager.getResponseList(response, IslLink.class);
            if (CollectionUtil.isEmpty(links)) {
                throw new ContentNotFoundException();
            }
            return islLinkConverter.toIslLinksInfo(links);
        }
        return null;
    }

    /**
     * Gets the isl link cost.
     *
     * @return the isl link cost
     */
    public LinkProps getIslLinkProps(LinkProps keys) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl(applicationProperties.getLinkProps());
        builder = setLinkProps(keys, builder);
        String fullUri = builder.build().toUriString();
        HttpResponse response = restClientManager.invoke(fullUri, HttpMethod.GET, "", "",
                applicationService.getAuthHeader());
        if (RestClientManager.isValidResponse(response)) {
            List<LinkProps> linkPropsResponse =
                    restClientManager.getResponseList(response, LinkProps.class);
            if (CollectionUtil.isEmpty(linkPropsResponse)) {
                throw new ContentNotFoundException();
            } else {
                return linkPropsResponse.get(0);
            }
        }
        return null;
    }

    /**
     * Gets the switch ports.
     *
     * @return the switch ports
     * @throws IntegrationException
     */
    public List<PortInfo> getSwitchPorts(final String switchId) throws IntegrationException {
    	HttpResponse response = null;
    	try {
//            HttpResponse response = restClientManager.invoke(applicationProperties.getSwitchPorts(),
//                    HttpMethod.GET, "", "", "");
            if (RestClientManager.isValidResponse(response)) {
                String responseEntity = IoUtil.toString(response.getEntity().getContent());
                JSONObject jsonObject = JsonUtil.toObject(responseEntity, JSONObject.class);
                return PortConverter.toPortsInfo(jsonObject, switchId);
            }
        } catch (IOException exception) {
            LOGGER.error("Exception in getSwitchPorts " + exception.getMessage(), exception);
            throw new IntegrationException(exception);
        }
        return null;
    }

    /**
     * Get custom switch name from file.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getCustomSwitchNameFromFile() {
        Map<String, String> csNames = new HashMap<String, String>();

        InputStream inputStream = null;
        String data = null;
        try {
            inputStream = new URL(applicationProperties.getSwitchDataFilePath()).openStream();
            if (inputStream != null) {
                data = IoUtil.toString(inputStream);

                if (data != null && !StringUtils.isEmpty(data)) {
                    csNames = JsonUtil.toObject(data, HashMap.class);
                }
            }
        } catch (Exception ex) {
            LOGGER.error(
                    "Inside getCustomSwitchNameFromFile unable to find switch file path Exception :",
                    ex);
        }
        return csNames;

    }

    /**
     * Update isl link props.
     * 
     * @param keys
     * @return link props
     */
    public LinkProps updateIslLinkProps(LinkProps keys) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl(applicationProperties.getLinkProps());
        builder = setLinkProps(keys, builder);
        String fullUri = builder.build().toUriString();
        HttpResponse response = restClientManager.invoke(fullUri, HttpMethod.PUT, "", "",
                applicationService.getAuthHeader());
        if (RestClientManager.isValidResponse(response)) {
            List<LinkProps> linkPropsResponse =
                    restClientManager.getResponseList(response, LinkProps.class);
            if (CollectionUtil.isEmpty(linkPropsResponse)) {
                throw new ContentNotFoundException();
            } else {
                return linkPropsResponse.get(0);
            }
        }
        return null;
    }

    /**
     * This Method is used to set link props.
     * 
     * @param keys
     * @param builder
     * @return UriComponentsBuilder
     */
    private UriComponentsBuilder setLinkProps(LinkProps keys, UriComponentsBuilder builder) {
        try {
            if (keys != null) {
                if (!keys.getSrc_switch().isEmpty())
                    builder.queryParam("src_switch",
                            URLEncoder.encode(keys.getSrc_switch(), "UTF-8"));
                if (!keys.getSrc_port().isEmpty())
                    builder.queryParam("src_port", URLEncoder.encode(keys.getSrc_port(), "UTF-8"));
                if (!keys.getDst_switch().isEmpty())
                    builder.queryParam("dst_switch",
                            URLEncoder.encode(keys.getDst_switch(), "UTF-8"));
                if (!keys.getDst_port().isEmpty())
                    builder.queryParam("dst_port", URLEncoder.encode(keys.getDst_port(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new ContentNotFoundException();
        }
        return builder;
    }
}
