package com.baojie.channelmanager.message;

import java.util.Map;
import java.util.Set;

public class MessageRequest {

	private String msgId;

	private String reqId; // 业务层ID，主要解决消息重试（重试会导致msgId发生变化）的问题

	private boolean retry; // 此消息是否为重试消息

	private String action;

	private Map<String, String> paramPair;

	public MessageRequest() {

	}

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public String getReqId() {
		return reqId;
	}

	public void setReqId(String reqId) {
		this.reqId = reqId;
	}

	public boolean getRetry() {
		return retry;
	}

	public void setRetry(boolean retry) {
		this.retry = retry;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Map<String, String> getParamPair() {
		return paramPair;
	}

	public void setParamPair(Map<String, String> paramPair) {
		this.paramPair = paramPair;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("RequestWapper [action=");
		stringBuilder.append(action);
		stringBuilder.append(", reqId=");
		stringBuilder.append(reqId);
		stringBuilder.append(", retry=");
		stringBuilder.append(retry);
		stringBuilder.append(", paramPairMap={");
		stringBuilder.append(getStringFromMap());
		stringBuilder.append("}");
		return stringBuilder.toString();
	}

	private String getStringFromMap() {
		if (null == paramPair) {
			return "NULL";
		}
		return getString();
	}

	private String getString() {
		StringBuilder mapString = new StringBuilder();
		Set<String> keys = paramPair.keySet();
		for (String key : keys) {
			mapString.append(key);
			mapString.append(":");
			mapString.append(paramPair.get(key));
			mapString.append(", ");
		}
		if (0 == mapString.length()) {
			mapString.append("");
		}
		return mapString.toString();
	}

}
