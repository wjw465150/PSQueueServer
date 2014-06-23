package com.leansoft.bigqueue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import wjw.psqueue.msg.ResQueueStatus;
import wjw.psqueue.msg.ResSubStatus;
import wjw.psqueue.msg.ResultCode;

import com.leansoft.bigqueue.utils.FileUtil;

public class FanOutQueueImplEx extends FanOutQueueImpl {
	String _queueName;

	public FanOutQueueImplEx(String queueDir, String queueName) throws IOException {
		super(queueDir, queueName);
		this._queueName = queueName;
	}

	public FanOutQueueImplEx(String queueDir, String queueName, int pageSize) throws IOException {
		super(queueDir, queueName, pageSize);
		this._queueName = queueName;
	}

	public long getNumberOfBackFiles() { //跟FanOutQueueImpl.getBackFileSize()的区别是:不包含index文件!
		return super.innerArray.dataPageFactory.getBackPageFileSet().size();
	}

	public void erase() throws IOException {
		String name = super.innerArray.arrayDirectory;

		super.removeAll();
		super.close();

		FileUtil.deleteDirectory(new File(name));
	}

	public void addFanout(String fanoutId) throws IOException {
		super.getQueueFront(fanoutId);
	}

	public void removeFanout(String fanoutId) throws IOException {
		this.queueFrontMap.remove(fanoutId).indexPageFactory.deleteAllPages();

		String dirName = innerArray.arrayDirectory + QUEUE_FRONT_INDEX_PAGE_FOLDER_PREFIX + fanoutId;
		FileUtil.deleteDirectory(new File(dirName));
	}

	public boolean containFanout(String fanoutId) {
		return this.queueFrontMap.containsKey(fanoutId);
	}

	public List<String> getAllFanoutNames() {
		List<String> result = new ArrayList<String>(this.queueFrontMap.size());
		for (QueueFront qf : this.queueFrontMap.values()) {
			result.add(qf.fanoutId);
		}

		return result;
	}

	public ResQueueStatus getQueueInfo() throws IOException {
		return new ResQueueStatus(ResultCode.SUCCESS, _queueName, super.size(), this.getRearIndex(), this.getFrontIndex());
	}

	public ResSubStatus getFanoutInfo(String fanoutId) throws IOException {
		return new ResSubStatus(ResultCode.SUCCESS, _queueName, fanoutId, super.size(fanoutId), this.getRearIndex(), this.getFrontIndex(fanoutId));
	}

	public static String getFanoutFolderPrefix() {
		return QUEUE_FRONT_INDEX_PAGE_FOLDER_PREFIX;
	}

	public static class QueueFilenameFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			try {
				File file = new File(dir, name);
				return file.isDirectory();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public static class SubFilenameFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			try {
				File file = new File(dir, name);
				return (file.isDirectory() && file.getName().startsWith(QUEUE_FRONT_INDEX_PAGE_FOLDER_PREFIX));
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
}
