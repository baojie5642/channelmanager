package com.baojie.channelmanager.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baojie.channelmanager.message.MessageResponse;
import com.baojie.channelmanager.util.SerializationUtil;
import com.baojie.channelmanager.util.UnitedCloudFutureReturnObject;
//@Sharable
public class HeartBeatClientHandler extends SimpleChannelInboundHandler<Object> {
	
	private final ConcurrentHashMap<String, UnitedCloudFutureReturnObject<MessageResponse>> messageFutureMap;
	
	private static final Logger log = LoggerFactory.getLogger(HeartBeatClientHandler.class);

	private HeartBeatClientHandler(final ConcurrentHashMap<String, UnitedCloudFutureReturnObject<MessageResponse>> messageFutureMap){
		this.messageFutureMap=messageFutureMap;
	}
	
	public static HeartBeatClientHandler create(final ConcurrentHashMap<String, UnitedCloudFutureReturnObject<MessageResponse>> messageFutureMap){
		return new HeartBeatClientHandler(messageFutureMap);
	}
	
	
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	log.info("通道channel："+ctx.channel().id()+"的激活时间是："+new Date());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	ctx.channel().close();
    	log.info("通道channel："+ctx.channel().id()+"的停止时间是："+new Date()+"已经调用channel.close()方法。");
        ctx.fireChannelInactive();
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof byte[]){
        	final byte[] bytesGetFromServer=(byte[])msg;
        	
        	if(checkLength(bytesGetFromServer)){
        		return;
        	}
        	final MessageResponse messageResponse=SerializationUtil.deserialize(bytesGetFromServer, MessageResponse.class);
        	if(checkNull(messageResponse)){
        		return;
        	}
        	UnitedCloudFutureReturnObject<MessageResponse> unitedCloudFutureReturnObject=messageFutureMap.get(messageResponse.getMsgId());
        	if(null!=unitedCloudFutureReturnObject){
        		unitedCloudFutureReturnObject.set(messageResponse);
        	}else {
        		log.error("从futureMap中获取的future为null，出错，请检查！！！");
			}
        	//log.info(messageResponse.getMsgId()+"  "+messageResponse.toString());
        }else {
			log.error("通道中传输过来的数据形式不可转化为byte[],请查看代码位置。["+msg+"].");
		}
    }
    
    private boolean checkLength(final byte[] bytes){
    	boolean isZero=false;
    	if(bytes.length==0){
    		log.error("被检查的数组长度为零，请检查……！！！");
    		isZero=true;
    	}else {
			isZero=false;
		}
    	return isZero;
    }
    
    private boolean checkNull(final MessageResponse messageResponse){
    	boolean isNull=false;
    	if(null==messageResponse){
    		log.error("反序列化生成的消息为null，请检查反序列化类的异常报错信息……！！！");
    		isNull=true;
    	}else {
			isNull=false;
		}
    	return isNull;
    }
    
    @Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.channel().close();
		cause.printStackTrace();
		log.error("通道channel："+ctx.channel().id()+"的异常时间是："+new Date()+"已经调用channel.close()方法。");
	}
}
