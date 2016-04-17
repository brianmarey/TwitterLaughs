package com.careydevelopment.twitterlaughs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.careydevelopment.propertiessupport.PropertiesFactory;
import com.careydevelopment.propertiessupport.PropertiesFactoryException;
import com.careydevelopment.propertiessupport.PropertiesFile;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterLaughs {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TwitterLaughs.class);
	
	private static final String COMEDIANS_FILE = "/etc/tomcat8/resources/comedians.properties";
	
	private static TwitterLaughs INSTANCE;
	private Twitter twitter;
	private Date twoDaysAgo;
	
	public static TwitterLaughs getInstance() throws TwitterLaughsException {
		if (INSTANCE == null) {
			synchronized (TwitterLaughs.class) {
				if (INSTANCE == null) {
					INSTANCE = new TwitterLaughs();
				}
			}
		}
		
		return INSTANCE;
	}
	
	
	/**
	 * Sets the current date to two days ago
	 */
	private void setDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -2);
		twoDaysAgo = calendar.getTime();
	}
	
	
	private TwitterLaughs() throws TwitterLaughsException{
		try {
			Properties props = PropertiesFactory.getProperties(PropertiesFile.TWITTER_PROPERTIES);
			
			String consumerKey=props.getProperty("brianmcarey.consumerKey");
			String consumerSecret=props.getProperty("brianmcarey.consumerSecret");
	    	String accessToken = props.getProperty("brianmcarey.accessToken");
	    	String accessSecret = props.getProperty("brianmcarey.accessSecret");
			
	    	ConfigurationBuilder builder = new ConfigurationBuilder();
	    	builder.setOAuthConsumerKey(consumerKey);
	    	builder.setOAuthConsumerSecret(consumerSecret);
	    	builder.setOAuthAccessToken(accessToken);
	    	builder.setOAuthAccessTokenSecret(accessSecret);
	    	Configuration configuration = builder.build();
	    	TwitterFactory factory = new TwitterFactory(configuration);
	    	twitter = factory.getInstance();
		} catch (PropertiesFactoryException pe) {
			throw new TwitterLaughsException("Problem reading Twitter properties!",pe);
		}
	}
	
	
	public List<String> getLaughs() throws TwitterLaughsException {
		List<String> ids = new ArrayList<String>();
		
		//set the date to two days ago
		setDate();
		
		//get the list of comedians
		List<String> comedians = getComedians();
		
		//get the tweets from each comedian
		List<Status> tweets = getFunnyTweets(comedians);
		
		/*for (Status status : tweets) {
			LOGGER.info(status.getText() + " " + status.getRetweetCount());
		}*/
		
		//translate the tweets into IDs
		ids = getIdsOfTweets(tweets);
		
		return ids;
	}

	
	private List<String> getIdsOfTweets(List<Status> tweets) {
		List<String> ids = new ArrayList<String>();
		
		for (Status status : tweets) {
			ids.add((new Long(status.getId()).toString()));
		}
		
		return ids;
	}
	
	
	private List<Status> getFunnyTweets(List<String> comedians) throws TwitterLaughsException {
		List<Status> tweets = new ArrayList<Status>();
		
		//get the tweets for each comedian
		for (String comedian : comedians) {
			tweets.addAll(getTweets(comedian));
		}
		
		//sort the tweets by number of retweets
		Collections.sort(tweets, new TweetsComparator());
		
		return tweets;
	}
	
	/**
	 * Gets all the tweets for the specified comedian
	 */
	private List<Status> getTweets(String comedian) throws TwitterLaughsException {
		List<Status> tweets = new ArrayList<Status>();
		
		LOGGER.info("Getting tweets for " + comedian);
		
		try {
			//this uses the Twitter REST API
			ResponseList<Status> statuses = twitter.getUserTimeline(comedian);
			
			if (statuses != null) {
				
				//step thru each tweet to be sure it qualifies
				for (int i=0;i<statuses.size();i++) {
					Status status = statuses.get(i);
					
					//ignore reply tweets
					if (status.getInReplyToScreenName() == null) {
						Date tweetedAt = status.getCreatedAt();
						
						//the tweet must have been posted within the past two days
						if (tweetedAt.after(twoDaysAgo)) {
							int likes = status.getFavoriteCount();
							int retweets = status.getRetweetCount();
							
							//needs to be popular based on likes and retweets
							if (likes + retweets > 300) {
								LOGGER.info("Status is " + status.getText());
								tweets.add(status);
							}
						}
					}
				}
			}
		} catch (TwitterException te) {
			throw new TwitterLaughsException("Problem getting timelines!",te);
		}

		return tweets;
	}
	
	
	/**
	 * Reads the text file to get the names of the comedians
	 */
	private List<String> getComedians() throws TwitterLaughsException {
		List<String> comedians = new ArrayList<String>();
		
		try(BufferedReader br = new BufferedReader(new FileReader(COMEDIANS_FILE))) {
		    for(String line; (line = br.readLine()) != null; ) {
		        comedians.add(line);
		        LOGGER.info("just added " + line);
		    }
		} catch (IOException ie) {
			throw new TwitterLaughsException("Can't find comedians properties file!", ie);
		}
		
		return comedians;
	}
	
	
	public static void main(String[] args) {
		try {
			TwitterLaughs laughs = TwitterLaughs.getInstance();
			List<String> ids = laughs.getLaughs();
		} catch (TwitterLaughsException te) {
			te.printStackTrace();
		}
	}
	
	
	/**
	 * Used to sort tweets in descending order based on retweet count
	 */
	private static class TweetsComparator implements Comparator<Status> {
		
		public int compare(Status status1, Status status2) {
			Integer retweets1 = status1.getRetweetCount();
			Integer retweets2 = status2.getRetweetCount();
			
			return retweets2.compareTo(retweets1);
		}
	}
}
