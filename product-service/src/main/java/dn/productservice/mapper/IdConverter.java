package dn.productservice.mapper;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public interface IdConverter {

    default UUID fromString(String uuid) {
        return UUID.fromString(uuid);
    }

    default List<UUID> fromStringList(List<String> uuids) {
        return uuids.stream()
                .map(this::fromString)
                .toList();
    }
}
