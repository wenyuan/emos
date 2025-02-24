package com.example.emos.wx.db.dao;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Repository
public class MessageDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    public String insert(MessageEntity entity){
        // 把北京时间转换成格林尼治时间
        Date sendTime = entity.getSendTime();
        sendTime = DateUtil.offset(sendTime, DateField.HOUR,8);
        entity.setSendTime(sendTime);
        entity = mongoTemplate.save(entity);
        return entity.get_id();
    }

    /**
     * 按分页查询数据
     * @param userId
     * @param start  起始位置（必须是long否则会报错）
     * @param length 偏移量
     * @return
     */
    public List<HashMap> searchMessageByPage(int userId, long start, int length) {
        JSONObject json = new JSONObject();
        json.set("$toString", "$_id");
        Aggregation aggregation = Aggregation.newAggregation(
                // 数据类型转换：message 中的 _id 转成字符串，存为变量 id
                Aggregation.addFields().addField("id").withValue(json).build(),
                // 关联 message_ref 集合，将 message.id 和 message_ref.messageId 联接，查询结果放到变量 ref
                Aggregation.lookup("message_ref","id","messageId","ref"),
                // 查询发送给指定用户的消息
                Aggregation.match(Criteria.where("ref.receiverId").is(userId)),
                // 数据降序排序
                Aggregation.sort(Sort.by(Sort.Direction.DESC,"sendTime")),
                // 分页
                Aggregation.skip(start),
                Aggregation.limit(length)
        );
        // 参数2：原始集合  参数3：查询出来的数据泛型
        AggregationResults<HashMap> results = mongoTemplate.aggregate(aggregation,"message", HashMap.class);
        List<HashMap> list = results.getMappedResults();
        list.forEach(one->{
            // 提取出引用字段里的值（可能是多条记录），塞到 HashMap
            List<MessageRefEntity> refList = (List<MessageRefEntity>) one.get("ref");
            MessageRefEntity entity = refList.get(0);
            boolean readFlag = entity.getReadFlag();
            String refId = entity.get_id();
            one.put("readFlag", readFlag);
            one.put("refId", refId);
            one.remove("ref");
            one.remove("_id");
            // 把格林尼治时间转换成北京时间
            Date sendTime = (Date) one.get("sendTime");
            sendTime = DateUtil.offset(sendTime,DateField.HOUR,-8);

            // 如果是今天的消息，只需要显示时间；如果不是今天的消息，只需要显示日期
            String today = DateUtil.today();
            if (today.equals(DateUtil.date(sendTime).toDateStr())) {
                one.put("sendTime",DateUtil.format(sendTime,"HH:mm"));
            } else {
                one.put("sendTime",DateUtil.format(sendTime,"yyyy/MM/dd"));
            }
        });
        return list;
    }

    /**
     * 根据 id 查询 message
     * @param id
     * @return
     */
    public HashMap searchMessageById(String id){
        HashMap map = mongoTemplate.findById(id, HashMap.class, "message");
        Date sendTime = (Date) map.get("sendTime");
        // 转回北京时间
        sendTime = DateUtil.offset(sendTime, DateField.HOUR, -8);
        map.replace("sendTime",DateUtil.format(sendTime, "yyyy-MM-dd HH:mm"));
        return map;
    }
}
