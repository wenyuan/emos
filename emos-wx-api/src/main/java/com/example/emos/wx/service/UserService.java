package com.example.emos.wx.service;

import com.example.emos.wx.db.pojo.TbUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public interface UserService {
    public int registerUser(String registerCode, String code, String nickname, String photo);

    public Set<String> searchUserPermissions(int userId);

    /**
     *
     * @param code 微信小程序临时授权字符串
     * @return userId
     */
    public Integer login(String code);

    public TbUser searchById(int userId);

    public String searchUserHiredate(int userId);

    public HashMap searchUserSummary(int userId);

    public ArrayList<HashMap> searchUserGroupByDept(String keyword);

    public ArrayList<HashMap> searchMembers(List param);

    public List<HashMap> selectUserPhotoAndName(List param);

    public String searchMemberEmail(int id);
}
