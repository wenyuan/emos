package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRefDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    public String insert(MessageRefEntity entity) {
        entity = mongoTemplate.save(entity);
        return entity.get_id();
    }

    /**
     * 查询未读消息数量
     * @param userId
     * @return
     */
    public long searchUnreadCount(int userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("readFlag").is(false).and("receiverId").is(userId));
        long count = mongoTemplate.count(query, MessageRefEntity.class);
        return count;
    }

    /**
     * 查询新接收消息数量
     * 思路：仅作修改，不做查询，因为 Mongo 在修改后会返回修改的记录数
     * @param userId
     * @return
     */
    public long searchLastCount(int userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("lastFlag").is(true).and("receiverId").is(userId));
        Update update = new Update();
        update.set("lastFlag", false);
        UpdateResult result = mongoTemplate.updateMulti(query, update, "message_ref");
        long rows = result.getModifiedCount();
        return rows;
    }

    /**
     * 把未读消息变更为已读消息
     * @param id
     * @return
     */
    public long updateUnreadMessage(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("readFlag", true);
        // 根据主键最多查到并修改一条数据，所以用 .updateFirst()
        UpdateResult result = mongoTemplate.updateFirst(query, update, "message_ref");
        long rows = result.getModifiedCount();
        return rows;
    }

    /**
     * 根据 ID 删除 ref 消息
     * 场景：不能删除消息主体，否则其他人就收不到了
     * @param id
     * @return
     */
    public long deleteMessageRefById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        DeleteResult result = mongoTemplate.remove(query,"message_ref");
        long rows = result.getDeletedCount();
        return rows;
    }

    /**
     * 删除某个用户全部消息
     * 场景：删除某个员工后，需要删除跟他相关的 ref 消息
     * @param userId
     * @return
     */
    public long deleteUserMessageRef(int userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("receiverId").is(userId));
        DeleteResult result=mongoTemplate.remove(query,"message_ref");
        long rows=result.getDeletedCount();
        return rows;
    }
}
