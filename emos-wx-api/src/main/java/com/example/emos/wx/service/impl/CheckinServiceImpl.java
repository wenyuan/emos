package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbFaceModel;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.task.EmailTask;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
@Scope("prototype")  // 发邮件功能需要异步执行，因此需要是多例
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    @Autowired
    private SystemConstants constants;

    @Autowired
    private TbHolidaysDao holidaysDao;

    @Autowired
    private TbWorkdayDao workdayDao;

    @Autowired
    private TbCheckinDao checkinDao;

    @Autowired
    private TbFaceModelDao faceModelDao;

    @Autowired
    private TbCityDao cityDao;

    @Autowired
    private TbUserDao userDao;

    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;

    @Value("${emos.email.hr}")
    private String hrEmail;

    @Value("${emos.code}")
    private String code;

    @Autowired
    private EmailTask emailTask;


    @Override
    public String validCanCheckIn(int userId, String date) {
        boolean bool_1 = holidaysDao.searchTodayIsHolidays() != null ? true : false;
        boolean bool_2 = workdayDao.searchTodayIsWorkday() != null ? true : false;
        String type = "工作日";
        // 常规判断
        if (DateUtil.date().isWeekend()) {
            type = "节假日";
        }
        // 修正：特殊的日子（这两个bool不能同时为true）
        if (bool_1) {
            type = "节假日";
        } else if (bool_2) {
            type = "工作日";
        }

        if (type.equals("节假日")) {
            return "节假日不需要考勤";
        } else {
            DateTime now = DateUtil.date();
            String start = DateUtil.today() + " " + constants.attendanceStartTime;
            String end = DateUtil.today() + " " + constants.attendanceEndTime;
            // 字符串转日期对象
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if (now.isBefore(attendanceStart)) {
                return "没到上班考勤开始时间";
            }
            else if (now.isAfter(attendanceEnd)) {
                return "超过了上班考勤结束时间";
            } else {
                HashMap map = new HashMap();
                map.put("userId", userId);
                map.put("date", date);
                map.put("start", start);
                map.put("end", end);
                boolean bool = checkinDao.haveCheckin(map) != null ? true : false;
                return bool ? "今日已经考勤，不用重复考勤" : "可以考勤";
            }
        }
    }

    @Override
    public void checkin(HashMap param) {
        // 当前时间
        Date d1 = DateUtil.date();
        // 上班时间
        Date d2 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceTime);
        // 上班考勤结束时间
        Date d3 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);
        int status = 1;
        if (d1.compareTo(d2) <= 0) {
            status=1; // 正常签到
        } else if (d1.compareTo(d2) > 0 && d1.compareTo(d3) < 0) {
            status=2; // 迟到
        } else {
            throw new EmosException("超出考勤时间段，无法考勤");
        }
        int userId = (Integer) param.get("userId");
        String faceModel = faceModelDao.searchFaceModel(userId);
        if (faceModel == null) {
            throw new EmosException("不存在人脸模型");
        } else {
            // 图片保存在本地的地址
            String path = (String)param.get("path");
            HttpRequest request = HttpUtil.createPost(checkinUrl);
            request.form("photo", FileUtil.file(path),"targetModel", faceModel);
            request.form("code", code);
            HttpResponse response = request.execute();
            if (response.getStatus() != 200) {
                // 发送告警邮件
                HashMap<String, String> map = userDao.searchNameAndDept(userId);
                String name = map.get("name");
                String deptName = map.get("dept_name");
                deptName = deptName != null ? deptName : "";
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(hrEmail);
                message.setSubject("员工" + name + "签到失败");
                message.setText(deptName + "员工" + name + "，" + DateUtil.format(new Date(), "yyyy年MM月dd日") + "，签到时人脸识别服务异常，请及时与运维人员联系！");
                emailTask.sendAsync(message);

                log.error("人脸识别服务异常");
                throw new EmosException("人脸识别服务异常");
            }
            String body = response.body();
            if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".equals(body)){
                throw new EmosException(body);
            } else if ("False".equals(body)) {
                throw new EmosException("签到无效，非本人签到");
            } else if ("True".equals(body)) {
                String city= (String) param.get("city");
                String district= (String) param.get("district");
                String address= (String) param.get("address");
                String country= (String) param.get("country");
                String province= (String) param.get("province");
                // 保存签到记录
                TbCheckin entity = new TbCheckin();
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);
                // int -> byte：当前Java类中是int类型，pojo类中是byte类型，MySQL中是tinyint
                entity.setStatus((byte) status);
                entity.setRisk(0);
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                checkinDao.insert(entity);
            }
        }
    }

    @Override
    public void createFaceModel(int userId, String path) {
        HttpRequest request = HttpUtil.createPost(createFaceModelUrl);
        request.form("photo", FileUtil.file(path));
        request.form("code", code);
        HttpResponse response = request.execute();
        String body = response.body();
        if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".equals(body)) {
            throw new EmosException(body);
        } else {
            TbFaceModel entity = new TbFaceModel();
            entity.setUserId(userId);
            entity.setFaceModel(body);
            faceModelDao.insert(entity);
        }
    }

    @Override
    public HashMap searchTodayCheckin(int userId) {
        HashMap map = checkinDao.searchTodayCheckin(userId);
        return map;
    }

    @Override
    public long searchCheckinDays(int userId) {
        long days = checkinDao.searchCheckinDays(userId);
        return days;
    }

    @Override
    public ArrayList<HashMap> searchWeekCheckin (HashMap param) {
        // 查询本周考勤情况
        ArrayList<HashMap> checkinList = checkinDao.searchWeekCheckin(param);
        // 查询本周特殊的节假日和工作日
        ArrayList holidaysList = holidaysDao.searchHolidaysInRange(param);
        ArrayList workdayList = workdayDao.searchWorkdayInRange(param);
        // 获取本周开始和结束日期对象
        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
        DateTime endDate = DateUtil.parseDate(param.get("endDate").toString());
        // 生成本周七天的日期对象
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);
        // 存储本周七天的考勤结果
        ArrayList<HashMap> list = new ArrayList<>();
        // 遍历判断本周每一天是工作日还是节假日
        range.forEach(one->{
            String date=one.toString("yyyy-MM-dd");
            String type = "工作日";
            if (one.isWeekend()) {
                type="节假日";
            }
            if(holidaysList != null && holidaysList.contains(date)) {
                type="节假日";
            } else if(workdayList!=null && workdayList.contains(date)) {
                type="工作日";
            }
            String status = ""; // 考勤时间结束前、未来的日期，考勤状态默认都是空字符串
            // one：本周的某一天   DateUtil.date()：当天
            if (type.equals("工作日") && DateUtil.compare(one, DateUtil.date()) <= 0) {
                status = "缺勤";
                boolean flag = false;  // 标识有没有查到今天的考勤记录
                for (HashMap<String,String> map: checkinList) {
                    if (map.containsValue(date)) {
                        status = map.get("status");
                        flag = true;
                        break;
                    }
                }
                // 当天考勤结束时间
                DateTime endTime = DateUtil.parse(DateUtil.today()+" "+constants.attendanceEndTime);
                // 当天日期字符串
                String today = DateUtil.today();
                if(date.equals(today) && DateUtil.date().isBefore(endTime) && flag == false){
                    status="";
                }
            }
            HashMap map = new HashMap();
            map.put("date", date);
            map.put("status", status);
            map.put("type", type);
            map.put("day", one.dayOfWeekEnum().toChinese("周")); // 星期几 -> 周几
            list.add(map);
        });
        return list;
    }

    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        // 查询月考勤和查询周考勤的基本代码相同，区别仅在查询参数（起始日期）
        return this.searchWeekCheckin(param);
    }

}
