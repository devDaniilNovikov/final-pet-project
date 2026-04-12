package dn.productservice.utils;

import lombok.experimental.UtilityClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@UtilityClass
public class IdConverter {

    public UUID fromString(String uuid) {
        return UUID.fromString(uuid);
    }

    public List<UUID> fromStringList(List<String> uuids) {
        return uuids.stream()
                .map(IdConverter::fromString)
                .toList();
    }
}
