package com.joshlong.twitter.organizer

import com.joshlong.twitter.TwitterPinboardOrganizerProperties
import com.joshlong.twitter.api.BearerTokenInterceptor
import com.joshlong.twitter.api.SimpleTwitterClient
import com.joshlong.twitter.api.Tweet
import com.joshlong.twitter.api.TwitterClient
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import pinboard.Bookmark
import pinboard.PinboardClient
import java.util.*

/**
 * The goal of this program is to take a Pinboard
 * item that was saved and has the description.
 */
@Configuration
class OrganizerConfiguration(private val twitterOrganizerProperties: TwitterPinboardOrganizerProperties) {

	// todo extract out the RestTemplate and the BaseTwitterClient into a separate auto-configuration
	private val authenticatedRestTemplate = RestTemplate()
			.apply {
				val bearerTokenInterceptor = BearerTokenInterceptor(
						twitterOrganizerProperties.consumerKey, twitterOrganizerProperties.consumerSecret)
				interceptors.add(bearerTokenInterceptor)
			}

	@Bean
	fun twitterClient(twitterOrganizerProperties: TwitterPinboardOrganizerProperties): TwitterClient =
			SimpleTwitterClient(this.authenticatedRestTemplate)
}

@Component
class TwitterOrganizer(
		private val twitterClient: TwitterClient,
		private val pinboardClient: PinboardClient) :
		ApplicationListener<ApplicationReadyEvent> {

	private val tags = arrayOf("twis")

	private fun subtractDaysFrom(numberOfDays: Int, d: Date) =
			GregorianCalendar
					.getInstance()
					.apply {
						val other = GregorianCalendar
								.getInstance()
								.apply {
									time = d
								}
						set(Calendar.DAY_OF_YEAR, other.get(Calendar.DAY_OF_YEAR) - numberOfDays)
					}
					.time

	override fun onApplicationEvent(event: ApplicationReadyEvent) {
		val tweetKey = "tweet"
		val bookmarkKey = "bookmark"
		//todo have something that goes back for 2 years in a while loop
		val now = Date()
		val then = subtractDaysFrom(14, now)
		this.pinboardClient
				.getAllPosts(tag = this.tags, todt = now, fromdt = then)
				.filter { it.description!!.trim() == "twitter.com" }
				.map {
					val url = it.href!!
					var found = false
					val idStr = url.split("/status/")[1].filter {
						if (it == '?') {
							found = true
						}
						!found && Character.isDigit(it)
					}
					val id = java.lang.Long.parseLong(idStr)
					mapOf(tweetKey to id, bookmarkKey to it)
				}
				.parallelStream()
				.forEach {
					println( "processing on ${Thread.currentThread().name}")
					val bookmark = it[bookmarkKey] as Bookmark
					val tweet = this.twitterClient.getTweet(it[tweetKey] as Long)
					if (tweet != null) {
						println("updated the bookmark ${bookmark.href} to ${tweet.id} and ${tweet.text}")
						this.pinboardClient.updatePost(
								url = bookmark.href!!,
								description = tweet.text,
								extended = bookmark.extended!!,
								tags = bookmark.tags,
								dt = bookmark.time!!,
								shared = bookmark.shared,
								toread = bookmark.toread
						)
						println("updated the bookmark ${bookmark.href} to ${tweet.id} and ${tweet.text}")

					}
					else {
						println( "the tweet was null")
					}
				}
	}
}