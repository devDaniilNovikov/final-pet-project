package dn.notificationservice.dto;

import dn.notificationservice.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;



@Getter
@Setter
@AllArgsConstructor
public class NotificationMapDto {

    private  Map<UUID, NotificationStatus> notificationStatusMap = new TreeMap<>();
}
