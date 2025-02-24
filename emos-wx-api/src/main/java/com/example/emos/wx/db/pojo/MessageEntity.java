package com.example.emos.wx.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

/**
 * 存储消息主体
 */
@Data
@Document(collection = "message")
public class MessageEntity implements Serializable {
    /**
     * Mongo自动生成的主键值
     */
    @Id
    private String _id;

    /**
     * 设置唯一性索引，防止消息被重复消费
     * 场景：第一个消费者获取消息后，还没来得及发送 ack 提醒删除消息，第二轮轮询时间到了，触发第二个消费者工作
     */
    @Indexed(unique = true)
    private String uuid;

    /**
     * 发送者ID，即用户ID，如果是系统自动发出，这个ID值是0
     */
    @Indexed
    private Integer senderId;

    /**
     * 发送者的头像URL
     * 场景：在消息页面要显示发送人的头像
     */
    private String senderPhoto="https://static-1258386385.cos.ap-beijing.myqcloud.com/img/System.jpg";

    /**
     * 发送者名称，也就是用户姓名
     * 场景：在消息页面要显示发送人的名字
     */
    private String senderName;

    /**
     * 消息正文
     */
    private String msg;

    /**
     * 发送时间
     */
    @Indexed
    private Date sendTime;
}
