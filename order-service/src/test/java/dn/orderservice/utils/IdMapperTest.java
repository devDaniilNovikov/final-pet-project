package dn.orderservice.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdMapperTest {

    @Test
    void mapToUUIDFromString_validUUID_returnsUUID() {
        UUID expected = UUID.randomUUID();
        UUID result = IdMapper.mapToUUIDFromString(expected.toString());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void mapToUUIDFromString_invalidString_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> IdMapper.mapToUUIDFromString("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapToListUUIDFromString_validList_returnsUUIDList() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        List<UUID> result = IdMapper.mapToListUUIDFromString(List.of(id1.toString(), id2.toString()));

        assertThat(result).containsExactly(id1, id2);
    }

    @Test
    void mapToListUUIDFromString_emptyList_returnsEmptyList() {
        List<UUID> result = IdMapper.mapToListUUIDFromString(List.of());
        assertThat(result).isEmpty();
    }
}