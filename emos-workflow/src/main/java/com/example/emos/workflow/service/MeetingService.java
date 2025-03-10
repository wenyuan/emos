package com.example.emos.workflow.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface MeetingService {
    HashMap searchMeetingByInstanceId(String instanceId);

    HashMap searchMeetingByUUID(String uuid);

    Long searchRoomIdByUUID(String uuid);

    List<String> searchUserMeetingInMonth(HashMap param);

    void updateMeetingStatus(HashMap param);

    ArrayList<Integer> searchMeetingUnpresent(String uuid);

    int updateMeetingUnpresent(HashMap param);


}