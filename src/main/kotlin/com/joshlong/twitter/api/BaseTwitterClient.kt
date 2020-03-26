package com.joshlong.twitter.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.logging.LogFactory
import org.springframework.web.client.RestTemplate
import java.lang.Boolean
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


/**
 * A client to read tweets from a given user's timeline.
 *
 * @author Josh Long
 * @see <a href="https://developer.twitter.com/en/docs/basics/authentication/oauth-2-0/bearer-tokens">how to do OAuth authentication for Twitter's API</a>
 * @see <a href="https://developer.twitter.com/en/docs/tweets/timelines/com.joshlong.twitter.api-reference/get-statuses-user_timeline">the API for a user's timeline</a>
 */
open class SimpleTwitterClient(private val restTemplate: RestTemplate) : TwitterClient {

	private fun tweetProducer(username: String, sinceId: Long): String {
		val userTimelineUrl = "https://api.twitter.com/1.1/statuses/user_timeline.json"
		val sinceIdParam = if (sinceId > 0) "&since_id=${sinceId}" else ""
		val uri = "${userTimelineUrl}?include_rts=1&count=200&screen_name=${username}${sinceIdParam}"
		return restTemplate.getForEntity(uri, String::class.java).body!!
	}

	private val log = LogFactory.getLog(SimpleTwitterClient::class.java)
	private val formatterString = "EEE MMM d HH:mm:ss ZZ yyyy"
	private val objectMapper = ObjectMapper()

	private val formatter = SimpleDateFormat(formatterString)


	override fun getUserTimeline(username: String, sinceId: Long): List<Tweet> =
			parseJson(tweetProducer(username, sinceId))

	// todo incorporate the rate limiter insight when returning requests.
	//  \for now the trick is to only run the SI poller every 15 minutes,
	//  \which is the rate limiter window time anyway

/*	private fun getRateLimiterStatusForUserTimeline() =
			getRateLimiterStatusForFamily("statuses", "/statuses/user_timeline")

	private fun getRateLimiterStatusForFamily(family: String, endpoint: String): RateLimitStatus {
		val json = rateLimitStatusProducer()
		val jsonNode = objectMapper.readTree(json)
		val rlJson = jsonNode["resources"][family][endpoint]
		val limit = rlJson["limit"].intValue()
		val remaining = rlJson["remaining"].intValue()
		val reset = rlJson["reset"].longValue()
		return RateLimitStatus(limit, remaining, reset)
	}*/

	private fun <T> collectionFromAttribute(json: JsonNode, attribute: String, extractor: (JsonNode) -> T): List<T> =
			if (!json.has(attribute)) emptyList() else json[attribute].map { extractor(it) }

	private fun buildUserMentions(json: JsonNode) = collectionFromAttribute(json, "user_mentions") {
		UserMention(it["screen_name"].textValue(), it["name"].textValue(), java.lang.Long.parseLong(it["id_str"].textValue()))
	}

	private fun buildHashtags(json: JsonNode) = collectionFromAttribute(json, "hashtags") { Hashtag(it["text"].textValue()) }

	private fun buildUrls(json: JsonNode) = collectionFromAttribute(json, "urls") { URL(it["expanded_url"].textValue()) }

	private fun buildEntities(json: JsonNode) = Entities(buildHashtags(json), buildUserMentions(json), buildUrls(json))

	private fun buildUser(jsonNode: JsonNode): User {
		val url: String? = jsonNode["url"]?.textValue()
		return User(
				java.lang.Long.parseLong(jsonNode["id_str"].textValue()),
				jsonNode["name"].textValue(),
				jsonNode["screen_name"].textValue(),
				jsonNode["location"].textValue(),
				jsonNode["description"].textValue(),
				if (url != null) URL(url) else null
		)
	}

	private fun log(msg: String) {
		if (log.isDebugEnabled) log.debug(msg)
	}

	override fun getTweet(tweetId: Long): Tweet? {
//		{"created_at":"Tue Mar 24 07:40:17 +0000 2020","id":1242355542020030466,"id_str":"1242355542020030466","text":"@starbuxman I first use it in the winter of 2014\/2015. One friend of mine printed the documentation and created a s\u2026 https:\/\/t.co\/b46v7DEzGB","truncated":true,"entities":{"hashtags":[],"symbols":[],"user_mentions":[{"screen_name":"starbuxman","name":"Josh Long (\u9f99\u4e4b\u6625, \u091c\u094b\u0936, \u0414\u0436\u043e\u0448 \u041b\u043e\u043d\u0433,  \u062c\u0648\u0634 \u0644\u0648\u0646\u0642)","id":4324751,"id_str":"4324751","indices":[0,11]}],"urls":[{"url":"https:\/\/t.co\/b46v7DEzGB","expanded_url":"https:\/\/twitter.com\/i\/web\/status\/1242355542020030466","display_url":"twitter.com\/i\/web\/status\/1\u2026","indices":[117,140]}]},"source":"\u003ca href=\"https:\/\/mobile.twitter.com\" rel=\"nofollow\"\u003eTwitter Web App\u003c\/a\u003e","in_reply_to_status_id":1242353917691719680,"in_reply_to_status_id_str":"1242353917691719680","in_reply_to_user_id":4324751,"in_reply_to_user_id_str":"4324751","in_reply_to_screen_name":"starbuxman","user":{"id":2255336726,"id_str":"2255336726","name":"Vlad Mihalcea","screen_name":"vlad_mihalcea","location":"Cluj-Napoca, Rom\u00e2nia","description":"@Java Champion working on @Hypersistence Optimizer, database aficionado, author of High-Performance Java Persistence. Blogging at https:\/\/t.co\/Ad93bwQ6EN","url":"https:\/\/t.co\/dRXjE3szyV","entities":{"url":{"urls":[{"url":"https:\/\/t.co\/dRXjE3szyV","expanded_url":"http:\/\/vladmihalcea.com\/","display_url":"vladmihalcea.com","indices":[0,23]}]},"description":{"urls":[{"url":"https:\/\/t.co\/Ad93bwQ6EN","expanded_url":"https:\/\/vladmihalcea.com","display_url":"vladmihalcea.com","indices":[130,153]}]}},"protected":false,"followers_count":18665,"friends_count":160,"listed_count":436,"created_at":"Fri Dec 20 18:04:04 +0000 2013","favourites_count":23186,"utc_offset":null,"time_zone":null,"geo_enabled":false,"verified":false,"statuses_count":34076,"lang":null,"contributors_enabled":false,"is_translator":false,"is_translation_enabled":false,"profile_background_color":"C6E2EE","profile_background_image_url":"http:\/\/abs.twimg.com\/images\/themes\/theme2\/bg.gif","profile_background_image_url_https":"https:\/\/abs.twimg.com\/images\/themes\/theme2\/bg.gif","profile_background_tile":false,"profile_image_url":"http:\/\/pbs.twimg.com\/profile_images\/414824378761613313\/n6UwbwIo_normal.jpeg","profile_image_url_https":"https:\/\/pbs.twimg.com\/profile_images\/414824378761613313\/n6UwbwIo_normal.jpeg","profile_banner_url":"https:\/\/pbs.twimg.com\/profile_banners\/2255336726\/1546942544","profile_link_color":"1B95E0","profile_sidebar_border_color":"C6E2EE","profile_sidebar_fill_color":"DAECF4","profile_text_color":"663B12","profile_use_background_image":true,"has_extended_profile":true,"default_profile":false,"default_profile_image":false,"following":null,"follow_request_sent":null,"notifications":null,"translator_type":"none"},"geo":null,"coordinates":null,"place":null,"contributors":null,"is_quote_status":false,"retweet_count":1,"favorite_count":10,"favorited":false,"retweeted":false,"lang":"en"}
		val tweetUrl = "https://api.twitter.com/1.1/statuses/show.json?id={id}"
		val body = restTemplate.getForEntity(tweetUrl, String::class.java, tweetId).body!!
		val tree = objectMapper.readTree(body)
		return buildTweet(tree)
	}

	private fun buildTweet(tweetNode: JsonNode): Tweet {
		val createdAt: Date =
				if (tweetNode.has("created_at")) {
					val textValue = tweetNode["created_at"].textValue()
//						log("the value is $textValue")
					try {
						synchronized(this.formatter) {
							formatter.parse(textValue)
						}
					} catch (ex: Exception) {
						log("couldn't parse $textValue!")
						Date()
					}
				} else {
					log("there is no date!")
					Date()
				}

		return Tweet(
				createdAt,
				java.lang.Long.parseLong(tweetNode["id_str"].textValue()),
				tweetNode["text"].textValue(),
				Boolean.parseBoolean(tweetNode["truncated"].textValue()),
				tweetNode["in_reply_to_status_id_str"].textValue(),
				buildEntities(tweetNode["entities"]),
				buildUser(tweetNode["user"])
		)
	}

	private fun parseJson(json: String): List<Tweet> {
		val tweets = mutableListOf<Tweet>()
		val jsonNode: JsonNode = objectMapper.readTree(json)
		jsonNode.forEach { tweets.add(buildTweet(it)) }
		return tweets
	}

}