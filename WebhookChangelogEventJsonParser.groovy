package com.ceilfors.jenkins.plugins.jiratrigger.webhook

import com.atlassian.jira.rest.client.api.domain.ChangelogGroup
import com.atlassian.jira.rest.client.api.domain.ChangelogItem
import com.atlassian.jira.rest.client.api.domain.Issue
import com.atlassian.jira.rest.client.internal.json.ChangelogItemJsonParser
import com.atlassian.jira.rest.client.internal.json.IssueJsonParser
import com.atlassian.jira.rest.client.internal.json.JsonObjectParser
import com.atlassian.jira.rest.client.internal.json.JsonParseUtil
import org.codehaus.jettison.json.JSONException
import org.codehaus.jettison.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.ceilfors.jenkins.plugins.jiratrigger.webhook.WebhookJsonParserUtils.satisfyRequiredKeys

/**
 * @author ceilfors
 */
class WebhookChangelogEventJsonParser implements JsonObjectParser<WebhookChangelogEvent> {

    /**
     * Not using ChangelogJsonParser because it is expecting "created" field which is not
     * being supplied from webhook event.
     */
    private final ChangelogItemJsonParser changelogItemJsonParser = new ChangelogItemJsonParser()
    private final IssueJsonParser issueJsonParser = new IssueJsonParser(new JSONObject([:]), new JSONObject([:]))
    private static final Logger log = LoggerFactory.getLogger(WebhookChangelogEventJsonParser)

    @Override
    WebhookChangelogEvent parse(JSONObject webhookEvent) throws JSONException {
        satisfyRequiredKeys(webhookEvent)

        Collection<ChangelogItem> items = JsonParseUtil.parseJsonArray(
                webhookEvent.getJSONObject('changelog').getJSONArray('items'), changelogItemJsonParser)
        
        // Sanitize issue Json before parsing
        def issueJson = webhookEvent.getJSONObject('issue')
        sanitizeOriginalEstimateSeconds(issueJson)

        Issue issue
        // try {
	    issue = issueJsonParser.parse(issueJson)
	    // } catch (JSONException e){
        //     log.warn("Failed to parse issue: ${e.message}")
        //     issue = null
	    // }
        return new WebhookChangelogEvent(
                webhookEvent.getLong('timestamp'),
                webhookEvent.getString('webhookEvent'),
                issue,
                new ChangelogGroup(null, null, items)
        )
    }
    private void sanitizeOriginalEstimateSeconds(JSONObject issueJson){
        if (issueJson == null) return

        def fields = issueJson.optJSONObject('fields')
        def timetracking = fields?.optJSONObject('timetracking')
        
        if (timetracking != null && timetracking.has('originalEstimateSeconds')){
            def value = timetracking.get('originalEstimateSeconds')
            def numericValue = tryParseNumber(value)
            if(numericValue != null){
                timetracking.put('originalEstimateSeconds', numericValue)
            }else {
                log.warn("Failed to parse issue: ${value} it is not a number")
                timetracking.put('originalEstimateSeconds', 0)
            }
        }
        if (timetracking != null && timetracking.has('remainingEstimateSeconds')){
            def value = timetracking.get('remainingEstimateSeconds')
            def numericValue = tryParseNumber(value)
            if(numericValue != null){
                timetracking.put('remainingEstimateSeconds', numericValue)
            }else {
                log.warn("Failed to parse issue: ${value} it is not a number")
                timetracking.put('remainingEstimateSeconds', 0)
            }
        }
        if (timetracking != null && timetracking.has('timeSpentSeconds')){
            def value = timetracking.get('timeSpentSeconds')
            def numericValue = tryParseNumber(value)
            if(numericValue != null){
                timetracking.put('timeSpentSeconds', numericValue)
            }else {
                log.warn("Failed to parse issue: ${value} it is not a number")
                timetracking.put('timeSpentSeconds', 0)
            }
        }
    }

    private Number tryParseNumber(def value){
        try{
            if(value instanceof Number){
                return value
            } else if(value instanceof String){
                return value.isInteger() ? value.toInteger() : value.toDouble()
            }
        } catch (Exception e){
            //
        }
        return null
    }
}