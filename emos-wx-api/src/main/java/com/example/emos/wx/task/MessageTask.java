package com.example.emos.wx.task;

import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MessageService;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MessageTask {
    @Autowired
    private ConnectionFactory factory;

    @Autowired
    private MessageService messageService;

    /**
     * 同步发送消息
     * @param topic  主题
     * @param entity 消息对象
     */
    public void send(String topic, MessageEntity entity) {
        String id = messageService.insertMessage(entity);
        // 向 RabbitMQ 发送消息
        try (Connection connection = factory.newConnection();  // 创建连接
             Channel channel = connection.createChannel();     // 创建通道
        ) {
            channel.queueDeclare(topic, true, false, false, null);  // 连接队列
            HashMap map = new HashMap();
            map.put("messageId", id);
            AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().headers(map).build();
            channel.basicPublish("", topic, properties, entity.getMsg().getBytes());
            log.debug("消息发送成功");
        } catch (Exception e) {
            log.error("执行异常", e);
            throw new EmosException("向MQ发送消息失败");
        }
    }

    @Async  // 线程异步执行
    public void sendAsync(String topic, MessageEntity entity) {
        send(topic, entity);
    }

    /**
     * 收取消息队列里的消息
     * @param topic
     * @return
     */
    public int receive(String topic) {
        int i = 0;
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();
        ) {
            channel.queueDeclare(topic, true, false, false, null);
            // 从消息队列中接收消息
            while (true) {
                // 获取数据后，不需要自动返回 ack 应答，而是写入 ref 集合成功后手动返回 ack
                GetResponse response = channel.basicGet(topic, false);
                if (response != null) {
                    AMQP.BasicProperties properties = response.getProps();
                    Map<String, Object> map = properties.getHeaders();
                    String messageId = map.get("messageId").toString(); // Object -> String
                    byte[] body = response.getBody();
                    String message = new String(body);
                    log.debug("从RabbitMQ接收的消息：" + message);

                    MessageRefEntity entity = new MessageRefEntity();
                    entity.setMessageId(messageId);
                    entity.setReceiverId(Integer.parseInt(topic));
                    entity.setReadFlag(false);
                    entity.setLastFlag(true);
                    messageService.insertRef(entity);
                    long deliveryTag = response.getEnvelope().getDeliveryTag();
                    channel.basicAck(deliveryTag, false);
                    i++;
                } else {
                    // 没有消息可接收了，退出循环
                    break;
                }
            }
        } catch (Exception e) {
            log.error("执行异常", e);
            throw new EmosException("接收消息失败");
        }
        return i;
    }

    @Async
    public int receiveAsync(String topic) {
        return receive(topic);
    }

    public void deleteQueue(String topic){
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();
        ) {
            channel.queueDelete(topic);
            log.debug("消息队列成功删除");
        }catch (Exception e) {
            log.error("删除队列失败", e);
            throw new EmosException("删除队列失败");
        }
    }

    /**
     * 删除队列
     * @param topic
     */
    @Async
    public void deleteQueueAsync(String topic){
        deleteQueue(topic);
    }

}
