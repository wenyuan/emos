package com.example.emos.wx.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

/**
 * 存储消息接收人相关信息
 */
@Data
@Document(collection = "message_ref")
public class MessageRefEntity implements Serializable {
    @Id
    private String _id;

    @Indexed
    private String messageId;

    @Indexed
    private Integer receiverId;

    /**
     * 是否已读
     */
    @Indexed
    private Boolean readFlag;

    /**
     * 是否为新接收的消息
     * 场景：每次轮询查询新接收的消息数量后，需要把这些消息的该字段置为 false
     */
    @Indexed
    private Boolean lastFlag;
}
