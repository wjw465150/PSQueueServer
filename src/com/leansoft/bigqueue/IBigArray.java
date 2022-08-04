package com.leansoft.bigqueue;

import java.io.Closeable;
import java.io.IOException;

/**
 * Append Only Big Array ADT
 * 
 * @author bulldog
 *
 */
public interface IBigArray extends Closeable {
	
	public static final long NOT_FOUND = -1;
	
	/**
	 * Append the data into the head of the array
	 * 
	 * @param data binary data to append
	 * @return appended index
	 * @throws IOException if there is any IO error
	 */
	long append(byte[] data) throws IOException;
	
	
	/**
	 * Get the data at specific index
	 * 
	 * @param index valid data index
	 * @return binary data if the index is valid
	 * @throws IOException if there is any IO error
	 */
	byte[] get(long index) throws IOException;
	
	/**
	 * Get the timestamp of data at specific index,
	 * 
	 * this is the timestamp when the data was appended.
	 * 
	 * @param index valid data index
	 * @return timestamp when the data was appended
	 * @throws IOException if there is any IO error
	 */
	long getTimestamp(long index) throws IOException;

	/**
	 * The total number of items has been appended into the array
	 * 
	 * @return total number
	 */
	long size();
	
	
	/**
	 * Get the back data file size per page.
	 * 
	 * @return size per page
	 */
	int getDataPageSize();
	
	/**
	 * The head of the array.
	 * 
	 * This is the next to append index, the index of the last appended data 
	 * is [headIndex - 1] if the array is not empty.
	 * 
	 * @return an index
	 */
	long getHeadIndex();
	
	/**
	 * The tail of the array.
	 * 
	 * The is the index of the first appended data
	 * 
	 * @return an index
	 */
	long getTailIndex();
	
	/**
	 * Check if the array is empty or not
	 * 
	 * @return true if empty false otherwise
	 */
	boolean isEmpty();
	
	/**
	 * Check if the ring space of java long type has all been used up.
	 * 
	 * can always assume false, if true, the world is end:)
	 * 
	 * @return array full or not
	 */
	boolean isFull();
	
	/**
	 * Remove all data in this array, this will empty the array and delete all back page files.
	 * 
	 */
	void removeAll() throws IOException;
	
	/**
	 * Remove all data before specific index, this will advance the array tail to index and 
	 * delete back page files before index.
	 *
	 * @param index an index
	 * @throws IOException exception thrown if there was any IO error during the removal operation
	 */
	void removeBeforeIndex(long index) throws IOException;
	
	/**
	 * Remove all data before specific timestamp, this will advance the array tail and delete back page files
	 * accordingly.
	 * 
	 * @param timestamp a timestamp
	 * @throws IOException exception thrown if there was any IO error during the removal operation
	 */
	void removeBefore(long timestamp) throws IOException;
	
	/**
	 * Force to persist newly appended data,
	 * 
	 * normally, you don't need to flush explicitly since:
	 * 1.) BigArray will automatically flush a cached page when it is replaced out,
	 * 2.) BigArray uses memory mapped file technology internally, and the OS will flush the changes even your process crashes,
	 * 
	 * call this periodically only if you need transactional reliability and you are aware of the cost to performance.
	 */
	void flush();
	
	/**
	 * Find an index closest to the specific timestamp when the corresponding item was appended
	 * 
	 * @param timestamp when the corresponding item was appended
	 * @return an index
	 * @throws IOException exception thrown if there was any IO error during the getClosestIndex operation
	 */
	long findClosestIndex(long timestamp) throws IOException;
	
	
	/**
	 * Get total size of back files(index and data files) of the big array
	 * 
	 * @return total size of back files
	 * @throws IOException exception thrown if there was any IO error during the getBackFileSize operation
	 */
	long getBackFileSize() throws IOException;
	
	/**
	 * limit the back file size, truncate back file and advance array tail index accordingly,
	 * Note, this is a best effort call, exact size limit can't be guaranteed
	 * 
	 * @param sizeLimit the size to limit
	 * @throws IOException exception thrown if there was any IO error during the limitBackFileSize operation
	 */
	void limitBackFileSize(long sizeLimit) throws IOException;
	
	
	/**
	 * Get the data item length at specific index
	 * 
	 * @param index valid data index
	 * @return the length of binary data if the index is valid
	 * @throws IOException if there is any IO error
	 */
	int getItemLength(long index) throws IOException;
}
