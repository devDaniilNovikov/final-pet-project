package dn.accountservice.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.AfterDomainEventPublication;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Table(
    schema = "marketplace",
    name = "accounts",
    indexes = {
        @Index(name = "idx_accounts_username", columnList = "username"),
        @Index(name = "idx_accounts_email",    columnList = "email")
    }
)
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id",nullable = false)
    private UUID id;

    @Column(nullable = false,name = "username")
    private String username;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false,name = "email")
    private String email;

    @Column(nullable = false,name = "keycloak_id")
    private UUID keycloakId;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Version
    private Integer version;

    @OneToMany(mappedBy = "account",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<AddressEntity> addresses = new ArrayList<>();

    @PrePersist
    public void prePersist(){
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }




    @Embedded
    private BanInfo banInfo;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BanInfo implements Serializable{


        @Serial
        private static final long serialVersionUID = 1L;
        private String reason;
        private Instant unbanDate;
        private Boolean isBanned;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AccountEntity that = (AccountEntity) o;

        return new EqualsBuilder().append(keycloakId, that.keycloakId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(keycloakId).toHashCode();
    }
}
