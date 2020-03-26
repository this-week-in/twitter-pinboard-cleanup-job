package com.joshlong.twitter.api

import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.Disabled
import org.springframework.web.client.RestTemplate

@Disabled
class TwitterClientIntegrationTests {

	private val authenticatedRestTemplate =
			RestTemplate()
					.apply {
						val mutableMap = System.getenv()
						fun keyIfItExists(key: String): String = if (mutableMap.containsKey(key)) mutableMap[key]!! else ""
						val apiKey = keyIfItExists("TWITTER_TWI_CLIENT_KEY")
						val apiKeySecret = keyIfItExists("TWITTER_TWI_CLIENT_KEY_SECRET")
						interceptors.add(BearerTokenInterceptor(apiKey, apiKeySecret))
					}

	private val twitterClient = SimpleTwitterClient(authenticatedRestTemplate)

	@Test
	fun `should be able to get tweets from other users`() {
		val username = "SpringCentral"
		val timeline = this.twitterClient.getUserTimeline(username)
		Assert.assertTrue(timeline.isNotEmpty())
		timeline.forEach {
			println(it)
		}
	}

	@Test
	fun `should be able to get a single Tweet`() {
		val tweet = this.twitterClient.getTweet(1242355542020030466L)!!
		println("${tweet.id} ${tweet.text}")
		Assert.assertTrue(tweet.text.contains("One friend of mine printed the documentation"))
		Assert.assertTrue(tweet.entities.userMentions.isNotEmpty())
		Assert.assertTrue(tweet.entities.urls.isNotEmpty())
		Assert.assertTrue(tweet.inReplyToStatusId != null)
		Assert.assertTrue(tweet.user.screenName == "vlad_mihalcea")
	}

}