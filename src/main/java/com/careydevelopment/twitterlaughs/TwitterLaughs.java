package com.careydevelopment.twitterlaughs;

import java.util.ArrayList;
import java.util.Calendar;
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
	
	private static TwitterLaughs INSTANCE;
	private Twitter twitter;
	
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
		
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -2);
		Date twoDaysAgo = calendar.getTime();
		
		try {
			ResponseList<Status> statuses = twitter.getUserTimeline("meganamram");
			
			if (statuses != null) {
				for (int i=0;i<statuses.size();i++) {
					Status status = statuses.get(i);
					if (status.getInReplyToScreenName() == null && status.getURLEntities().length == 0) {
						Date tweetedAt = status.getCreatedAt();
						
						if (tweetedAt.after(twoDaysAgo)) {
							LOGGER.info("Status is " + status.getText());
						}
					}
				}
			}
			
		} catch (TwitterException te) {
			throw new TwitterLaughsException("Problem getting timelines!",te);
		}
		
		return ids;
	}
	
	
	public static void main(String[] args) {
		try {
			TwitterLaughs laughs = TwitterLaughs.getInstance();
			List<String> ids = laughs.getLaughs();
		} catch (TwitterLaughsException te) {
			te.printStackTrace();
		}
	}

}
