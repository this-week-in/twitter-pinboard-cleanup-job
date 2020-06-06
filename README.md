# Twitter Pinboard Cleanup Job 

![Build Status](https://github.com/this-week-in/twitter-pinboard-cleanup-job/workflows/CI/badge.svg)

This goes through all the tweets that i've bookmarked with Pinboard that didn't get a proper description, pulls down the relevant tweet and uses that tweet's text to enrich the Pinboard bookmark. It's run as a periodic job on platforms that support scheduled jobs like Cloud Foundry, Github Actions and Kubernetes.
 
Note: this should get merged into the `twitter-organizer-job` which organizes my Twitter followers and followees.






