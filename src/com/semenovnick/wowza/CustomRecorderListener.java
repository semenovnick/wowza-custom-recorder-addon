// ver 1.0 by semenovnick
package com.semenovnick.wowza;

import java.io.*;

import org.joda.time.*;

import com.wowza.wms.livestreamrecord.manager.*;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;

public class CustomRecorderListener implements IStreamRecorderActionNotify {
	private CustomFileVersionDelegate fileVersionDelegate = new CustomFileVersionDelegate();
	private DateTime lastSegmentBegin = null;
	private WMSLogger logger = WMSLoggerFactory.getLogger(this.getClass());
	public File oldFileWaiting = null;
	public File newFileWaiting = null;

	public void attempt(File oldFile, File newFile) {
		String newName = oldFile.getPath();
		try {
			Integer attempt = 0;
			while (!oldFile.renameTo(newFile) || attempt < 5) {
				attempt = attempt + 1;
				Thread.sleep(1000); // wait 1 seconds
			}
			newName = newFile.getPath();
			logger.info("CustomRecorderListener - File Successfully Renamed to after " + attempt.toString()
					+ " attemps: \n " + newName);
		} catch (InterruptedException ex) {
		}
	}

	public String renameOnCorrupt(IStreamRecorder recorder, DateTime segBegin, DateTime segEnd) {
		String newName = recorder.getCurrentFile();
		File oldFile = new File(newName);
		File newFile = new File(fileVersionDelegate.getFileNameAdvanced(recorder, segBegin, segEnd));
		boolean flag = oldFile.renameTo(newFile);
		if (flag) {
			newName = newFile.getPath();
			logger.info("CustomRecorderListener - File Successfully Renamed to: \n " + newName);
		} else {
			oldFileWaiting = oldFile;
			newFileWaiting = newFile;
			logger.info("CustomRecorderListener - File renaming failed, trying to rename after file become free...");
			Thread attemptToRename = new Thread(new Runnable() {
				public void run() {
					try {
						String newName = newFileWaiting.getPath();
						Integer attempt = 0;
						logger.info("CustomRecorderListener - Wait some second Trying to rename...");
						while (!oldFileWaiting.renameTo(newFileWaiting) && attempt < 20) {
							attempt = attempt + 1;
							Thread.sleep(5000); // wait 5 seconds
						}
						logger.info("CustomRecorderListener - File Successfully Renamed to after " + attempt.toString()
								+ " attemps: \n" + newName);
					} catch (InterruptedException ex) {
						logger.info("CustomRecorderListener - After 20 attempts renaming failed old FileName remains");
					}
				}
			});
			attemptToRename.start();
		}
		return newName;
	}

	@Override
	public void onSwitchRecorder(IStreamRecorder streamRecorder, IMediaStream mediaStream) {
		logger.info("CustomRecorderListener - RecordSwitched " + streamRecorder.getBaseFilePath() + " mediaStream "
				+ mediaStream.getContextStr());
	}

	@Override
	public void onStopRecorder(IStreamRecorder streamRecorder) {

		String newName = renameOnCorrupt(streamRecorder, lastSegmentBegin, streamRecorder.getEndTime());
		logger.info("CustomRecorderListener - Stopped with new fileName" + newName);
	}

	@Override
	public void onStartRecorder(IStreamRecorder streamRecorder) {
		logger.info("CustomRecorderListener - Start recording");

	}

	@Override
	public void onSplitRecorder(IStreamRecorder streamRecorder) {
		logger.info("CustomRecorderListener - Split at " + lastSegmentBegin.toString());
		String newName = renameOnCorrupt(streamRecorder, lastSegmentBegin, DateTime.now());
		logger.info("CustomRecorderListener - Save with new fileName" + newName);
		lastSegmentBegin = DateTime.now();
	}

	@Override
	public void onCreateRecorder(IStreamRecorder streamRecorder) {
		logger.info("CustomRecorderListener - Create recording for base file: " + streamRecorder.getBaseFilePath());

	}

	@Override
	public void onSegmentEnd(IStreamRecorder streamRecorder) {
		logger.info("CustomRecorderListener - Segment END! File name is " + streamRecorder.getCurrentFile());
	}

	@Override
	public void onSegmentStart(IStreamRecorder streamRecorder) {
		lastSegmentBegin = DateTime.now();
		logger.info("CustomRecorderListener - New segment at " + lastSegmentBegin.toString());
	}

};
