// ver 1.0
package com.semenovnick.wowza;

import java.io.*;

import org.joda.time.*;

import com.wowza.wms.livestreamrecord.manager.*;
import com.wowza.wms.logging.*;


public class CustomFileVersionDelegate implements IStreamRecorderFileVersionDelegate {
	private String YEAR_TAG = "${YYYY}";
	private String MONTH_TAG = "${MONTH}";
	private String DAY_TAG = "${DAY}";
	private String HOUR_TAG = "${HH}";
	private String MINUTES_TAG = "${MM}";
	private String SECONDS_TAG = "${SS}";
	private String MILLIS_TAG = "${millis}";
	private String SEGMENT_NUM_TAG = "${SegNum}";
	private String STREAMNAME_TAG = "${StreamName}";
	private String SEGMENT_DUR_TAG = "${SegDur}";
	private String SEGMENT_END_HH = "${SegEndHH}";
	private String SEGMENT_END_MM = "${SegEndMM}";
	private String SEGMENT_END_SS = "${SegEndSS}";

	public String getFilename(IStreamRecorder recorder) {
		return getFileNameAdvanced(recorder, null, null);
	}

	public String getFileNameAdvanced(IStreamRecorder recorder, DateTime creationTime, DateTime endingTime) {
		// This function will be used in CustomRecorderListener to rename file after record stopped
		String name;
		if (creationTime == null) {
			creationTime = DateTime.now();
		}

		try {
			File file = new File(recorder.getBaseFilePath());
			String oldBasePath = file.getParent();
			String oldName = file.getName();

			String oldExt = "";
			int oldExtIndex = oldName.lastIndexOf(".");
			if (oldExtIndex >= 0) {
				oldExt = oldName.substring(oldExtIndex);
				oldName = oldName.substring(0, oldExtIndex);
			}


			String newName = makeName(oldName, recorder, creationTime, endingTime);

			name = oldBasePath + "/" + newName + oldExt;
			file = new File(name);
			if (file.exists()) {
				file.delete();
			}

		} catch (Exception e) {
			WMSLoggerFactory.getLogger(CustomFileVersionDelegate.class)
					.error("LiveStreamRecordFileVersionDelegate.getFilename: " + e.toString());
			name = recorder.getBaseFilePath();
		}

		return name;
	}

	private String addZero(Integer value) {
		String result;
		if (value < 10) {
			result = "0" + String.valueOf(value);
		} else {
			result = String.valueOf(value);
		}
		return result;
	}

	private String makeName(String oldName, IStreamRecorder recorder, DateTime creationTime, DateTime endingTime) {

		String newName = oldName;
		Integer years = creationTime.getYearOfEra();
		Integer month = creationTime.getMonthOfYear();
		Integer day = creationTime.getDayOfMonth();
		Integer hours = creationTime.getHourOfDay();
		Integer minutes = creationTime.getMinuteOfHour();
		Integer seconds = creationTime.getSecondOfMinute();
		Integer millis = creationTime.getMillisOfSecond();
		Integer segNum = recorder.getSegmentNumber();
		Long segDur = recorder.getRecorderParams().segmentDuration;

		String streamName = recorder.getStreamName();

		Integer segEndHours = 0;
		Integer segEndMinutes = 0;
		Integer segEndSeconds = 0;

		if (endingTime == null) {
			int segDurSeconds = (int) (segDur / 1000) % 60;
			int segDurMinutes = (int) ((segDur / (1000 * 60)) % 60);
			int segDurHours = (int) ((segDur / (1000 * 60 * 60)) % 24);

			segEndHours = hours + segDurHours;
			segEndMinutes = minutes + segDurMinutes;
			segEndSeconds = seconds + segDurSeconds;
			if (segEndSeconds >= 60) {
				segEndSeconds = segEndSeconds - 60;
				segEndMinutes = segEndMinutes + 1;
			}

			if (segEndMinutes >= 60) {
				segEndMinutes = segEndMinutes - 60;
				segEndHours = segEndHours + 1;
			}
			if (segEndHours >= 24) {
				segEndHours = segEndHours - 24;
			}
		} else {
			segEndSeconds = endingTime.getSecondOfMinute();
			segEndMinutes = endingTime.getMinuteOfHour();
			segEndHours = endingTime.getHourOfDay();
		}

		newName = newName.replace(YEAR_TAG, String.valueOf(years));
		newName = newName.replace(MONTH_TAG, addZero(month));
		newName = newName.replace(DAY_TAG, addZero(day));
		newName = newName.replace(HOUR_TAG, addZero(hours));
		newName = newName.replace(MINUTES_TAG, addZero(minutes));
		newName = newName.replace(SECONDS_TAG, addZero(seconds));
		newName = newName.replace(MILLIS_TAG, addZero(millis));
		newName = newName.replace(SEGMENT_NUM_TAG, addZero(segNum));
		newName = newName.replace(SEGMENT_DUR_TAG, Long.toString(segDur));
		newName = newName.replace(STREAMNAME_TAG, streamName);
		newName = newName.replace(SEGMENT_END_HH, addZero(segEndHours));
		newName = newName.replace(SEGMENT_END_MM, addZero(segEndMinutes));
		newName = newName.replace(SEGMENT_END_SS, addZero(segEndSeconds));

		return newName;
	}
}
