package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * 持久层只需要定义接口，不需要实现类，因为 MyBatis 会通过动态代理去实现类
 */
@Mapper
public interface TbUserDao {
    public boolean haveRootUser();

    public int insert(HashMap param);

    // 如果查不到，返回的是 null，所以返回值类型只能用 Integer 而不是 int
    public Integer searchIdByOpenId(String openId);

    public Set<String> searchUserPermissions(int userId);

    public TbUser searchById(int userId);

    public HashMap searchNameAndDept(int userId);

    public String searchUserHiredate(int userId);

    public HashMap searchUserSummary(int userId);

    public ArrayList<HashMap> searchUserGroupByDept(String keyword);

    public ArrayList<HashMap> searchMembers(List param);

    public HashMap searchUserInfo(int userId);

    public int searchDeptManagerId(int id);

    public int searchGmId();

    public List<HashMap> selectUserPhotoAndName(List param);

    public String searchMemberEmail(int id);
}