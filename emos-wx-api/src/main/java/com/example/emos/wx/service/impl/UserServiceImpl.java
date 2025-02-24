package com.example.emos.wx.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.dao.TbDeptDao;
import com.example.emos.wx.db.dao.TbUserDao;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.UserService;
import com.example.emos.wx.task.MessageTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@Scope("prototype")  // 令牌刷新功能需要使用到 ThreadLocal，需要是多例
public class UserServiceImpl implements UserService {
    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

    // 需要调用 Dao 里的方法，因此需要它的引用变量
    @Autowired
    private TbUserDao userDao;

    @Autowired
    private TbDeptDao deptDao;

    @Autowired
    private MessageTask messageTask;

    /**
     * 这是一个私有的业务方法，将微信的临时授权转换成 openId 字符串
     * 因为该方法只希望在当前业务类中调用，其他类不需要调用，故不用在接口中声明抽象方法
     */
    private String getOpenId(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        HashMap map = new HashMap();
        map.put("appid", appId);
        map.put("secret", appSecret);
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String response = HttpUtil.post(url, map);
        JSONObject json = JSONUtil.parseObj(response);
        String openId = json.getStr("openid");
        if (openId == null || openId.length() == 0) {
            // 微信平台出现问题，抛出 RuntimeException
            throw new RuntimeException("临时登陆凭证错误");
        }
        return openId;
    }

    @Override
    public int registerUser(String registerCode, String code, String nickname, String photo) {
        // 如果邀请码是 000000，代表是超级管理员
        if(registerCode.equals("000000")) {
            // 查询超级管理员账户是否已经绑定
            boolean bool = userDao.haveRootUser();
            if (!bool) {
                // 把当前用户绑定到 ROOT 账户
                String openId = getOpenId(code);
                HashMap param=new HashMap();
                param.put("openId", openId);
                param.put("nickname", nickname);
                param.put("photo", photo);
                param.put("role", "[0]");  // 0 代表超级管理员的角色
                param.put("status", 1);
                param.put("createTime", new Date());
                param.put("root", true);
                userDao.insert(param);
                // 当前方法需要返回主键值，因此需要查一下
                int id = userDao.searchIdByOpenId(openId);

                // 注册后发送系统消息
                MessageEntity entity = new MessageEntity();
                entity.setSenderId(0);
                entity.setSenderName("系统消息");
                entity.setUuid(IdUtil.simpleUUID());
                entity.setMsg("欢迎您注册成为超级管理员，请及时更新你的员工个人信息。");
                entity.setSendTime(new Date());
                messageTask.sendAsync(id+"", entity); // int -> string
                return id;
            } else {
                // 如果 ROOT 已经绑定了，就抛出异常
                // 业务上出现问题，抛出 RuntimeException
                throw new EmosException("无法绑定超级管理员账号");
            }
        } else {
          // TODO... 普通员工的注册流程
        }
        return 0;
    }

    @Override
    public Set<String> searchUserPermissions(int userId) {
        Set<String> permissions=userDao.searchUserPermissions(userId);
        return permissions;
    }

    @Override
    public Integer login(String code) {
        String openId = getOpenId(code);
        Integer id = userDao.searchIdByOpenId(openId);
        if (id == null) {
            throw new EmosException("帐户不存在");
        }

        // 从消息队列中接收消息，转移到消息表
        // 在用户离线的过程中，系统发送的消息通知存放在 MQ 队列中，因此用户登录之后需要接收这些消息，然后存储到 Ref 集合中
        messageTask.receiveAsync(id+"");

        return id;
    }

    @Override
    public TbUser searchById(int userId) {
        TbUser user = userDao.searchById(userId);

        return user;
    }

    @Override
    public String searchUserHiredate(int userId) {
        String hiredate=userDao.searchUserHiredate(userId);
        return hiredate;
    }

    @Override
    public HashMap searchUserSummary(int userId) {
        HashMap map = userDao.searchUserSummary(userId);
        return map;
    }

    @Override
    public ArrayList<HashMap> searchUserGroupByDept(String keyword) {
        ArrayList<HashMap> list_1=deptDao.searchDeptMembers(keyword);
        ArrayList<HashMap> list_2=userDao.searchUserGroupByDept(keyword);
        for(HashMap map_1:list_1){
            long deptId=(Long)map_1.get("id");
            ArrayList members=new ArrayList();
            for(HashMap map_2:list_2){
               long id=(Long) map_2.get("deptId");
               if(deptId==id){
                   members.add(map_2);
               }
            }
            map_1.put("members",members);
        }
        return list_1;
    }

    @Override
    public ArrayList<HashMap> searchMembers(List param) {
        ArrayList<HashMap> list=userDao.searchMembers(param);
        return list;
    }

    @Override
    public List<HashMap> selectUserPhotoAndName(List param) {
        List<HashMap> list=userDao.selectUserPhotoAndName(param);
        return list;
    }

    @Override
    public String searchMemberEmail(int id) {
        String email=userDao.searchMemberEmail(id);
        return email;
    }

}
