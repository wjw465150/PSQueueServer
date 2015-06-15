package wjw.psqueue.msg;

import java.beans.ConstructorProperties;

public class ResultCode {
	public final static ResultCode SUCCESS = new ResultCode(0, "ok");
	public final static ResultCode CMD_INVALID = new ResultCode(1, "Invalid opt command!");
	public final static ResultCode INTERNAL_ERROR = new ResultCode(2, "internal error");
	public final static ResultCode AUTHENTICATION_FAILURE = new ResultCode(3, "authentication failure");
	public final static ResultCode QUEUE_NOT_EXIST = new ResultCode(4, "queue not exist");
	public final static ResultCode SUB_NOT_EXIST = new ResultCode(5, "Subscriber not exist");
	public final static ResultCode QUEUE_NAME_INVALID = new ResultCode(6, "invalid queue name!queue name not include:? \\ / : | < > * and _META_");
	public final static ResultCode SUB_NAME_INVALID = new ResultCode(7, "invalid Subscriber name!Subscriber name not include:? \\ / : | < > * and _META_");
	public final static ResultCode SUB_IS_EXIST = new ResultCode(8, "Subscriber is already exist");
	public final static ResultCode QUEUE_IS_EXIST = new ResultCode(9, "queue is already exist");
	public final static ResultCode QUEUE_IS_EMPTY = new ResultCode(10, "queue is empty");
	public final static ResultCode QUEUE_CREATE_ERROR = new ResultCode(11, "queue create error");
	public final static ResultCode QUEUE_REMOVE_ERROR = new ResultCode(12, "queue remove error");
	public final static ResultCode SUB_REMOVE_ERROR = new ResultCode(13, "Subscriber remove error");
	public final static ResultCode INDEX_OUT_OF_BOUNDS = new ResultCode(14, "index out of bounds");
	public final static ResultCode QUEUE_ADD_ERROR = new ResultCode(15, "queue add error");
	public final static ResultCode QUEUE_POLL_ERROR = new ResultCode(16, "queue poll error");
	public final static ResultCode ALL_MESSAGE_CONSUMED = new ResultCode(17, "all message consumed");
	public final static ResultCode QUEUE_CAPACITY_INVALID = new ResultCode(18, "queue capacity must >=1000000L AND <=1000000000L");
	public final static ResultCode SUB_TAILPOS_ERROR = new ResultCode(19, "Subscriber set tail pos error");
	
	public int code;
	public String msg;

	public ResultCode() {
	}

	@ConstructorProperties({ "code", "msg" })
	public ResultCode(final int code, final String msg) {
		this.code = code;
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResultCode [code=").append(code).append(", msg=").append(msg).append("]");
		return builder.toString();
	}

}
