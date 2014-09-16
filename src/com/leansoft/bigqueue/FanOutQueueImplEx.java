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
	public final static int MINI_TRUNCATE_SIZE = 100000;

	String _queueName;

	public String getQueueName() {
		return _queueName;
	}

	public FanOutQueueImplEx(String queueDir, String queueName) throws IOException {
		super(queueDir, queueName);
		this._queueName = queueName;
	}

	public FanOutQueueImplEx(String queueDir, String queueName, int pageSize) throws IOException {
		super(queueDir, queueName, pageSize);
		this._queueName = queueName;
	}

	//返回Queue占用的磁盘空间
	public long getDiskSize() { //跟BigArrayImpl.getBackFileSize()的区别是:不包含index文件!
		return super.innerArray.dataPageFactory.getBackPageFileSet().size();
	}

	//删除Queue下的所有东西(包括目录)
	public void erase() throws IOException {
		String name = super.innerArray.arrayDirectory;

		super.removeAll();
		super.close();

		FileUtil.deleteDirectory(new File(name));
	}

	//添加Sub
	public void addFanout(String fanoutId) throws IOException {
		super.getQueueFront(fanoutId);
	}

	//删除Sub
	public void removeFanout(String fanoutId) throws IOException {
		this.queueFrontMap.remove(fanoutId).indexPageFactory.deleteAllPages();

		String dirName = innerArray.arrayDirectory + QUEUE_FRONT_INDEX_PAGE_FOLDER_PREFIX + fanoutId;
		FileUtil.deleteDirectory(new File(dirName));
	}

	//是否包含指定的Sub
	public boolean containFanout(String fanoutId) {
		return this.queueFrontMap.containsKey(fanoutId);
	}

	//得到所有的Sub
	public List<String> getAllFanoutNames() {
		List<String> result = new ArrayList<String>(this.queueFrontMap.size());
		for (QueueFront qf : this.queueFrontMap.values()) {
			result.add(qf.fanoutId);
		}

		return result;
	}

	//返回Queue信息
	public ResQueueStatus getQueueInfo(long capacity) throws IOException {
		return new ResQueueStatus(ResultCode.SUCCESS, _queueName, capacity, super.size(), this.getRearIndex(), this.getFrontIndex());
	}

	//返回Sub信息
	public ResSubStatus getFanoutInfo(String fanoutId, long capacity) throws IOException {
		return new ResSubStatus(ResultCode.SUCCESS, _queueName, capacity, fanoutId, super.size(fanoutId), this.getRearIndex(), this.getFrontIndex(fanoutId));
	}

	//返回Sub的目录前缀
	public static String getFanoutFolderPrefix() {
		return QUEUE_FRONT_INDEX_PAGE_FOLDER_PREFIX;
	}

	//限制容量
	public void limitCapacity(long capacity) throws IOException {
		long toTruncateSize = this.size() - capacity;
		if (toTruncateSize < MINI_TRUNCATE_SIZE) {
			return;
		}

		try {
			this.innerArray.arrayWriteLock.lock();

			long tailIndex = this.innerArray.getTailIndex();
			if ((tailIndex + toTruncateSize) < 0) {
				tailIndex = toTruncateSize + tailIndex - Long.MAX_VALUE;
			} else {
				tailIndex = toTruncateSize + tailIndex;
			}
			this.innerArray.removeBeforeIndex(tailIndex);

			for (QueueFront qf : this.queueFrontMap.values()) {
				try {
					qf.writeLock.lock();
					qf.validateAndAdjustIndex();
				} finally {
					qf.writeLock.unlock();
				}
			}
			
		} finally {
			this.innerArray.arrayWriteLock.unlock();
		}

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
