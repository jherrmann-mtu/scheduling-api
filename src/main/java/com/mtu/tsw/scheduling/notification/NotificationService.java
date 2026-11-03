package com.mtu.tsw.scheduling.notification;

import org.springframework.stereotype.Service;

import com.mtu.tsw.scheduling.schedule.AppointmentEntity;
import com.mtu.tsw.scheduling.user.UserEntity;

@Service 
public class NotificationService {
    
    public void sendEditNotification(AppointmentEntity before, AppointmentEntity after) {}

    public void sendCreateNotification(AppointmentEntity appointment) {}

    public void sendHostCancelNotification(AppointmentEntity appointment) {}

    public void sendGuestCancelNotification(AppointmentEntity appointment, UserEntity cancelUser) {}

}
