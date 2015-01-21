package com.growthbeat.analytics;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.growthbeat.CatchableThread;
import com.growthbeat.GrowthbeatCore;
import com.growthbeat.Logger;
import com.growthbeat.Preference;
import com.growthbeat.analytics.model.ClientEvent;
import com.growthbeat.analytics.model.ClientTag;
import com.growthbeat.analytics.options.TrackEventOption;
import com.growthbeat.http.GrowthbeatHttpClient;
import com.growthbeat.utils.AppUtils;
import com.growthbeat.utils.DeviceUtils;

public class GrowthAnalytics {

	public static final String LOGGER_DEFAULT_TAG = "GrowthAnalytics";
	public static final String HTTP_CLIENT_DEFAULT_BASE_URL = "https://api.analytics.growthbeat.com/";
	public static final String PREFERENCE_DEFAULT_FILE_NAME = "growthanalytics-preferences";

	private static final String EVENT = "Event";
	private static final String TAG = "Tag";
	private static final String GENERAL = "General";
	private static final String GAME = "Game";
	private static final String SOCIAL = "Social";
	private static final String GROWTHPUSH = "GrowthPush";
	private static final String GROWTHMAIL = "GrowthMail";

	private static final GrowthAnalytics instance = new GrowthAnalytics();
	private final Logger logger = new Logger(LOGGER_DEFAULT_TAG);
	private final GrowthbeatHttpClient httpClient = new GrowthbeatHttpClient(HTTP_CLIENT_DEFAULT_BASE_URL);
	private final Preference preference = new Preference(PREFERENCE_DEFAULT_FILE_NAME);

	private String applicationId;
	private String credentialId;

	private GrowthAnalytics() {
		super();
	}

	public static GrowthAnalytics getInstance() {
		return instance;
	}

	public void initialize(final Context context, final String applicationId, final String credentialId) {

		GrowthbeatCore.getInstance().initialize(context, applicationId, credentialId);

		this.applicationId = applicationId;
		this.credentialId = credentialId;
		this.preference.setContext(GrowthbeatCore.getInstance().getContext());

	}

	public void trackEvent(String openEventId) {
		trackEvent(openEventId, null);
	}

	public void trackEvent(String openEventId, Map<String, String> properties) {
		trackEvent(openEventId, properties, null);
	}

	public void trackEvent(final String eventId, final Map<String, String> properties, final TrackEventOption option) {

		this.logger.info(String.format("Track event... (eventId: %s, properties: %s)", eventId, properties));

		new Thread(new Runnable() {
			@Override
			public void run() {

				ClientEvent referencedClientEvent = ClientEvent.load(eventId);
				if ((option == TrackEventOption.ONCE) && referencedClientEvent != null) {
					GrowthAnalytics.this.logger.info(String.format("This event send only once. (eventId: %s)", eventId));
					return;
				}

				if (referencedClientEvent == null && (option == TrackEventOption.COUNTER))
					properties.put("first_time", null);

				try {

					ClientEvent clientEvent = ClientEvent.create(GrowthbeatCore.getInstance().waitClient().getId(), eventId, properties);
					ClientEvent.save(clientEvent);
					GrowthAnalytics.this.logger.info("save event .");

					GrowthAnalytics.this.logger.info(String.format("Tracking event success. (id: %s)", clientEvent.getId()));
				} catch (GrowthAnalyticsException e) {
					GrowthAnalytics.this.logger.info(String.format("Tracking event fail. %s", e.getMessage()));
				}
			}
		}).start();

	}

	public void setTag(final String tagId) {
		setTag(tagId, null);
	}

	public void setTag(final String tagId, final String value) {

		this.logger.info(String.format("Set tag... (tagId: %s, value: %s)", tagId, value));

		new Thread(new Runnable() {
			@Override
			public void run() {

				ClientTag referecedClientTag = ClientTag.load(tagId);
				if (referecedClientTag != null && (value != null && value.equals(referecedClientTag.getValue()))) {
					GrowthAnalytics.this.logger.info(String.format("Already exists tag... (tagId: %s, value: %s)", tagId, value));
					return;
				}

				try {
					ClientTag clientTag = ClientTag.create(GrowthbeatCore.getInstance().waitClient().getId(), tagId, value);
					GrowthAnalytics.this.logger.info("Setting tag success.");
					ClientTag.save(clientTag);
				} catch (GrowthAnalyticsException e) {
					GrowthAnalytics.this.logger.info(String.format("Setting tag fail. %s", e.getMessage()));
				}
			}
		}).start();

	}

	public void setUserId(String userId) {
		setTag(String.format("%s:%s", GENERAL, "UserID"), userId);
	}

	public void setAdvertisingId(String advertisingId) {
		setTag(String.format("%s:%s", GENERAL, "AdvertisingID"), advertisingId);
	}

	public void setAge(int age) {
		setTag(String.format("%s:%s", GENERAL, "Age"), String.valueOf(age));
	}

	public void setGender(String gender) {
		setTag(String.format("%s:%s", GENERAL, "Gender"), String.valueOf(gender));
	}

	public void setLocale(String locale) {
		setTag(String.format("%s:%s", GENERAL, "Locale"), locale);
	}

	public void setLanguage(String language) {
		setTag(String.format("%s:%s", GENERAL, "langugage"), language);
	}

	public void setOS(String os) {
		setTag(String.format("%s:%s", GENERAL, "OS"), os);
	}

	public void setTimeZone(String timeZone) {
		setTag(String.format("%s:%s", GENERAL, "TimeZone"), timeZone);
	}

	public void setAppVersion(String appVersion) {
		setTag(String.format("%s:%s", GENERAL, "AppVersion"), appVersion);
	}

	public void setName(String name) {
		setTag(String.format("%s:%s", GENERAL, "Name"), name);
	}

	public void setRandom(String random) {
		setTag(String.format("%s:%s", GENERAL, "Random"), random);
	}

	public void setLevel(String level) {
		setTag(String.format("%s:%s", GENERAL, "Level"), level);
	}

	public void setDevelopment(String development) {
		setTag(String.format("%s:%s", GENERAL, "Development"), development);
	}

	public void open() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("referrer", null);
		String openEventId = String.format("%s:%s", GENERAL, "Open");
		trackEvent(openEventId, properties);
		trackEvent(String.format("%s:%s", GENERAL, "Install"), properties, TrackEventOption.ONCE);
	}

	public void close() {
		ClientEvent openEvent = ClientEvent.load(String.format("%s:%s", GENERAL, "Open"));
		Map<String, String> properties = new HashMap<String, String>();
		if (openEvent != null) {
			Date now = new Date();
			long time = now.getTime() - openEvent.getCreated().getTime();
			properties.put("time", String.valueOf(time));
		}
		trackEvent(String.format("%s:%s", GENERAL, "Close"), properties);
	}

	public void purchase(int price, String category, String product) {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("Price", String.valueOf(price));
		properties.put("Category", category);
		properties.put("Product", product);
		trackEvent(String.format("%s:%s", GENERAL, "Purchase"), properties);
	}

	public void setDeviceTags() {

		new Thread(new Runnable() {

			@Override
			public void run() {

				if (GrowthbeatCore.getInstance().getContext() == null)
					throw new IllegalStateException("GrowthPush is not initialized.");

				setOS("Android " + DeviceUtils.getOsVersion());
				setLanguage(DeviceUtils.getLanguage());
				setTimeZone(DeviceUtils.getTimeZone());
				setTag(String.format("%s:%s", GENERAL, "TimeZoneOffset"), DeviceUtils.getTimeZoneOffset());
				setAppVersion(AppUtils.getaAppVersion(GrowthbeatCore.getInstance().getContext()));

			}

		}).start();

	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public String getCredentialId() {
		return credentialId;
	}

	public void setCredentialId(String credentialId) {
		this.credentialId = credentialId;
	}

	public Logger getLogger() {
		return logger;
	}

	public GrowthbeatHttpClient getHttpClient() {
		return httpClient;
	}

	public Preference getPreference() {
		return preference;
	}

	private static class Thread extends CatchableThread {

		public Thread(Runnable runnable) {
			super(runnable);
		}

		@Override
		public void uncaughtException(java.lang.Thread thread, Throwable e) {
			String message = "Uncaught Exception: " + e.getClass().getName();
			if (e.getMessage() != null)
				message += "; " + e.getMessage();
			GrowthAnalytics.getInstance().getLogger().warning(message);
			e.printStackTrace();
		}

	}

}
