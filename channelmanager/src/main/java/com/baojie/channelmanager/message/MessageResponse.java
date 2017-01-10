package com.baojie.channelmanager.message;

public class MessageResponse {

	private String msgId;

	private Integer code;

	private String description;

	private String body;

	public MessageResponse() {

	}

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "ResponseWapper [code=" + code + ", description=" + description + ", body=" + body + "]";
	}

}
